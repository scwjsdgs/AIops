import json
import os
from langchain_classic.memory import ConversationBufferMemory
from typing import Dict

MEMORY_DIR = "./memory_storage"

class MemoryManager:
    def __init__(self):
        os.makedirs(MEMORY_DIR, exist_ok=True)
        self._memories: Dict[str, ConversationBufferMemory] = {}

    def get_memory(self, task_id: str) -> ConversationBufferMemory:
        if task_id not in self._memories:
            memory = self._load_from_disk(task_id)
            if memory is None:
                memory = ConversationBufferMemory(
                    memory_key="chat_history",
                    return_messages=True,
                    output_key="output"
                )
            self._memories[task_id] = memory
        return self._memories[task_id]

    def save_memory(self, task_id: str):
        memory = self._memories.get(task_id)
        if memory:
            data = {
                "messages": [
                    {"type": msg.type, "content": msg.content}
                    for msg in memory.chat_memory.messages
                ]
            }
            with open(os.path.join(MEMORY_DIR, f"{task_id}.json"), "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)

    def _load_from_disk(self, task_id: str):
        path = os.path.join(MEMORY_DIR, f"{task_id}.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
            memory = ConversationBufferMemory(
                memory_key="chat_history",
                return_messages=True,
                output_key="output"
            )
            from langchain_core.messages import HumanMessage, AIMessage, SystemMessage
            type_map = {
                "human": HumanMessage,
                "ai": AIMessage,
                "system": SystemMessage
            }
            for msg in data["messages"]:
                cls = type_map.get(msg["type"])
                if cls:
                    memory.chat_memory.add_message(cls(content=msg["content"]))
            return memory
        return None

memory_manager = MemoryManager()