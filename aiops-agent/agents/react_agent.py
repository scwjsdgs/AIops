import json
import asyncio
from typing import AsyncGenerator
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, ToolMessage, SystemMessage
from config import config
from tools.agent_tools import create_tools
from agents.memory_manager import memory_manager
from services.callback import callback_service
from models import Alert

SYSTEM_PROMPT = """你是一位资深的 SRE 运维专家，拥有丰富的工具可调用。请严格遵循 ReAct 推理模式：

**可用工具清单**：
1. search_knowledge_base：查询内部故障手册/SOP
2. get_service_status：查看服务当前运行状态（副本数、就绪情况、镜像版本）
3. analyze_logs：获取服务最近错误日志
4. execute_repair_action：执行重启（restart）、清理缓存（clear_cache）等常规动作
5. scale_service：调整实例数量（扩缩容）
6. rollback_version：回滚服务版本（高危！需谨慎）
7. request_human_approval：执行高风险操作前先申请人工确认

**推理循环格式**：
Thought: 分析当前告警，决定如何排查。
Action: 选择一个工具，并传入参数。
Observation: 观察工具返回结果，判断是否解决问题。
重复直到根因明确且修复完成，最后输出包含根因、操作步骤、验证结果的总结报告。

**重要安全规则**：
- 在执行 rollback_version，或执行 scale_service 且实例数变化超过 50% 时，必须先调用 request_human_approval 获取批准。判断比例前，先用 get_service_status 拿到当前副本数。
- 修复完成后，务必调用 get_service_status 验证服务已恢复正常。
- 工具返回失败时，禁止在最终报告里声称操作成功；必须如实说明失败原因和当前状态。
- 如果知识库和日志都查不到线索，请基于常识给出稳妥建议。
"""


async def run_react_loop(
    task_id: str,
    user_input: str,
    max_iterations: int = None,
) -> AsyncGenerator[str, None]:
    """流式执行 ReAct 循环，逐条产出 SSE 事件（JSON 字符串，不含换行）。

    换行由传输层（main.py）统一负责，这里只产出事件本身。
    """
    if max_iterations is None:
        max_iterations = config.AGENT_MAX_ITERATIONS

    memory = memory_manager.get_memory(task_id)
    llm = ChatOpenAI(
        model=config.LLM_MODEL,
        openai_api_key=config.LLM_API_KEY,
        openai_api_base=config.LLM_BASE_URL,
        temperature=0,
        # 关掉流式：部分 OpenAI 兼容网关在 streaming 下对 tool_calls 的增量分片
        # 处理不完整，会导致工具调用丢失。这里的"流式"本就是逐事件而非逐 token。
        streaming=False,
        timeout=config.LLM_TIMEOUT_SECONDS,
        max_retries=config.LLM_MAX_RETRIES,
    )
    tools = create_tools(task_id)

    # 关键：必须把工具声明绑定到 LLM 上。
    # 不绑定的话，请求里没有任何 tools 声明，网关永远不会返回 tool_calls，
    # 下面的循环会退化成「LLM 直接输出一段散文就结束」，所有工具形同虚设。
    llm_with_tools = llm.bind_tools(tools) if tools else llm
    tools_dict = {t.name: t for t in tools if getattr(t, "name", None)}

    history = memory.chat_memory.messages if memory else []
    messages = (
        [SystemMessage(content=SYSTEM_PROMPT)]
        + list(history)
        + [HumanMessage(content=user_input)]
    )

    iteration = 0
    final_answer = None

    while iteration < max_iterations:
        response = await llm_with_tools.ainvoke(messages)
        ai_msg = response
        messages.append(ai_msg)

        content = ai_msg.content
        tool_calls = getattr(ai_msg, "tool_calls", None) or []

        if content:
            yield json.dumps({"type": "thought", "data": content}, ensure_ascii=False)

        if not tool_calls:
            final_answer = content
            yield json.dumps({"type": "final", "data": final_answer}, ensure_ascii=False)
            break

        for tc in tool_calls:
            tool_name = tc["name"]
            tool_args = tc["args"]
            yield json.dumps(
                {"type": "action", "data": {"tool": tool_name, "args": tool_args}},
                ensure_ascii=False,
            )

            tool_func = tools_dict.get(tool_name)
            if tool_func is None:
                observation = f"未知工具: {tool_name}，请改用可用工具清单中的工具。"
                yield json.dumps({"type": "error", "data": observation}, ensure_ascii=False)
            else:
                try:
                    observation = await tool_func.ainvoke(tool_args)
                except Exception as e:
                    observation = f"工具执行失败: {str(e)}"
                    yield json.dumps({"type": "error", "data": observation}, ensure_ascii=False)

            yield json.dumps(
                {"type": "observation", "data": str(observation)}, ensure_ascii=False
            )
            messages.append(ToolMessage(content=str(observation), tool_call_id=tc["id"]))

        iteration += 1

    if final_answer is None:
        final_answer = f"达到最大迭代次数（{max_iterations}），未能完成处理。"
        yield json.dumps({"type": "final", "data": final_answer}, ensure_ascii=False)

    memory.chat_memory.add_user_message(user_input)
    memory.chat_memory.add_ai_message(final_answer or "")
    memory_manager.save_memory(task_id)


async def run_agent_for_alert(task_id: str, alert: Alert) -> str:
    """非流式入口：跑完整个 ReAct 循环并把最终报告回传给 Java。"""
    user_input = (
        f"服务 {alert.serviceName} 发生告警：{alert.title} - {alert.description}"
        f"，详情：{alert.detail or '无'}"
    )
    final = None

    try:
        async with asyncio.timeout(config.AGENT_TIMEOUT_SECONDS):
            async for event in run_react_loop(task_id, user_input):
                data = json.loads(event)
                if data.get("type") == "final":
                    final = data.get("data")
                    break
    except TimeoutError:
        final = f"处理超时（超过 {config.AGENT_TIMEOUT_SECONDS} 秒），未能完成分析。"
        await callback_service.complete_task(task_id, final, status="FAILED")
        return final

    if final is None:
        final = "处理失败，未获得最终报告。"
    await callback_service.complete_task(task_id, final)
    return final
