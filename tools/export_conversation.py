"""Claude Code 세션 기록(jsonl)을 제출용 텍스트로 변환. 24자리 hex(DB 비밀번호 형태)와 password= 값은 마스킹.
사용: python3 tools/export_conversation.py > /tmp/ai-conversation.md
"""
import glob
import json
import os
import re
import sys

PROJECT = os.path.expanduser("~/.claude/projects/-Volumes-development-kayple")
SECRET_PATTERNS = [
    (re.compile(r"\b[0-9a-f]{24}\b"), "***REDACTED***"),
    # 검증용 grep 명령에 들어간 비밀번호 앞 8자리 조각
    (re.compile(r"(\\\|)0[0-9a-f]{7}\b"), r"\1***REDACTED***"),
    # 과제 안내 페이지 토큰(그 아래에서 SSH 키 zip 을 받을 수 있음)
    (re.compile(r"\bkmj-[a-z0-9]{10}\b"), "kmj-***REDACTED***"),
    # DB 비밀번호 앞부분 조각(따옴표·공백 뒤 6자리 이상 hex 로 시작하는 토큰)
    (re.compile(r"(?<=[\"'\s|])0[0-9]{2}[a-f][0-9a-f]{2,}(?=[|\s\"'])"), "***REDACTED***"),
    (re.compile(r"((?:PASSWORD|password)\s*[=:]\s*)[^\s\"',}]+"), r"\1***REDACTED***"),
]


# 안내 페이지·키 zip 링크가 들어간 줄은 통째로 삭제
LINK_LINE = re.compile(r"^.*https://recruit\.kayple\.com/kmj-.*$\n?", re.M)
# 이전에 내보낸 대화를 다시 붙여넣은 블록은 중복이라 한 줄로 축약
NESTED_EXPORT = re.compile(r"<pasted_content[^>]*>(?:(?!</pasted_content).)*?### Claude.*?</pasted_content[^>]*>", re.S)


def redact(text: str) -> str:
    text = LINK_LINE.sub("", text)
    text = NESTED_EXPORT.sub("[이전에 내보낸 대화 일부를 붙여넣음 — 중복이라 생략]", text)
    for pattern, repl in SECRET_PATTERNS:
        text = pattern.sub(repl, text)
    return text


def texts(content):
    if isinstance(content, str):
        yield content
        return
    for block in content or []:
        if block.get("type") == "text":
            yield block["text"]
        elif block.get("type") == "tool_use":
            cmd = block.get("input", {}).get("command") or block.get("input", {}).get("file_path") or ""
            yield f"[도구 사용: {block.get('name')}] {str(cmd)[:300]}"


def main():
    files = sorted(glob.glob(f"{PROJECT}/*.jsonl"), key=os.path.getmtime)
    for path in files:
        with open(path, encoding="utf-8") as f:
            for line in f:
                entry = json.loads(line)
                role = entry.get("type")
                if role not in ("user", "assistant") or entry.get("isMeta"):
                    continue
                parts = [t for t in texts(entry.get("message", {}).get("content")) if t.strip()]
                if parts:
                    label = "사용자" if role == "user" else "Claude"
                    sys.stdout.write(f"\n### {label}\n" + redact("\n".join(parts)) + "\n")


if __name__ == "__main__":
    main()
