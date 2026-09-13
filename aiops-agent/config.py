import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # LLM 配置（以 Qwen 为例，使用 OpenAI 兼容接口）
    LLM_API_KEY: str = os.getenv("LLM_API_KEY", "your-api-key-here")
    LLM_BASE_URL: str = os.getenv("LLM_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "qwen-plus")

    # 单次 LLM 调用的超时（秒）与失败重试次数
    LLM_TIMEOUT_SECONDS: int = int(os.getenv("LLM_TIMEOUT_SECONDS", "60"))
    LLM_MAX_RETRIES: int = int(os.getenv("LLM_MAX_RETRIES", "2"))

    # 单个告警任务（整个 ReAct 循环）的整体超时（秒）
    AGENT_TIMEOUT_SECONDS: int = int(os.getenv("AGENT_TIMEOUT_SECONDS", "300"))

    # ReAct 最大迭代轮数。多步运维通常需要「查状态 -> 查日志 -> 重启 -> 验证 -> 总结」，
    # 5 轮不够用，会中途撞上限。
    AGENT_MAX_ITERATIONS: int = int(os.getenv("AGENT_MAX_ITERATIONS", "8"))

    # 向量库配置
    RAG_ENABLED: bool = os.getenv("RAG_ENABLED", "true").lower() in ("1", "true", "yes", "on")
    VECTOR_STORE_DIR: str = os.getenv("VECTOR_STORE_DIR", "./rag/chroma_db")
    DOCUMENTS_DIR: str = os.getenv("DOCUMENTS_DIR", "./rag/documents")

    # Java 后端地址（ops_agent 监听 8081）
    JAVA_BASE_URL: str = os.getenv("JAVA_BASE_URL", "http://localhost:8081")

    # 与 Java 约定的内部共享密钥。回调接口必须携带 X-Internal-Token，
    # 因为这些接口能触发 restart/scale 等真实运维操作，不能裸奔放行。
    JAVA_INTERNAL_TOKEN: str = os.getenv("JAVA_INTERNAL_TOKEN", "dev-internal-token")

    # 调用 Java 的 HTTP 超时（秒）。read 必须足够长：
    # restart_service 在 Java 侧本身就要跑好几秒（缩容、等待、扩容）。
    HTTP_CONNECT_TIMEOUT: float = float(os.getenv("HTTP_CONNECT_TIMEOUT", "5"))
    HTTP_READ_TIMEOUT: float = float(os.getenv("HTTP_READ_TIMEOUT", "60"))
    HTTP_WRITE_TIMEOUT: float = float(os.getenv("HTTP_WRITE_TIMEOUT", "10"))

    class Config:
        env_file = ".env"

config = Settings()
