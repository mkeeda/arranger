#!/usr/bin/env python3
import os
import sys
import json
import argparse
from pathlib import Path

def find_session_logs(brain_dir: Path, max_sessions: int = 5):
    """
    Find recent session log files in the brain directory.
    Sort by directory modification time.
    """
    if not brain_dir.exists():
        return []

    log_files = []
    for item in brain_dir.iterdir():
        if item.is_dir() and item.name != "scratch" and item.name != "tempmediaStorage":
            transcript_path = item / ".system_generated" / "logs" / "transcript.jsonl"
            if transcript_path.exists():
                mtime = item.stat().st_mtime
                log_files.append((mtime, item.name, transcript_path))
    
    log_files.sort(key=lambda x: x[0], reverse=True)
    return log_files[:max_sessions]

def parse_transcript(transcript_path: Path):
    """
    Parse transcript.jsonl and extract user input messages and key agent events.
    """
    user_inputs = []
    step_count = 0
    try:
        with open(transcript_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    data = json.loads(line)
                    step_count += 1
                    step_type = data.get("type", "")
                    
                    if step_type == "USER_INPUT":
                        content = data.get("content", "")
                        # Handle content if it's a list or string
                        if isinstance(content, list):
                            text_parts = []
                            for part in content:
                                if isinstance(part, dict) and "text" in part:
                                    text_parts.append(part["text"])
                                elif isinstance(part, str):
                                    text_parts.append(part)
                            text_content = "\n".join(text_parts)
                        else:
                            text_content = str(content)

                        user_inputs.append({
                            "step_index": data.get("step_index", step_count),
                            "content": text_content
                        })
                except json.JSONDecodeError:
                    continue
    except Exception as e:
        sys.stderr.write(f"Error reading {transcript_path}: {e}\n")

    return user_inputs

def main():
    parser = argparse.ArgumentParser(description="Analyze Antigravity session logs for retrospective.")
    parser.add_argument("--brain-dir", type=str, default=os.path.expanduser("~/.gemini/antigravity/brain"),
                        help="Path to the brain directory containing session logs.")
    parser.add_argument("--max-sessions", type=int, default=5,
                        help="Maximum number of recent session logs to inspect.")
    parser.add_argument("--session-id", type=str, default=None,
                        help="Specific session ID to analyze.")
    parser.add_argument("--json", action="store_true",
                        help="Output in JSON format.")

    args = parser.parse_args()
    brain_dir = Path(args.brain_dir)

    if args.session_id:
        target_log = brain_dir / args.session_id / ".system_generated" / "logs" / "transcript.jsonl"
        if not target_log.exists():
            sys.stderr.write(f"Session log not found: {target_log}\n")
            sys.exit(1)
        sessions = [(0, args.session_id, target_log)]
    else:
        sessions = find_session_logs(brain_dir, max_sessions=args.max_sessions)

    results = []

    for _, session_id, log_path in sessions:
        inputs = parse_transcript(log_path)
        results.append({
            "session_id": session_id,
            "log_path": str(log_path),
            "user_inputs_count": len(inputs),
            "user_inputs": inputs
        })

    if args.json:
        print(json.dumps(results, ensure_ascii=False, indent=2))
    else:
        print(f"=== Antigravity Session Logs Retrospective Analysis ({len(results)} sessions) ===")
        for res in results:
            print(f"\n--- Session ID: {res['session_id']} ({res['user_inputs_count']} user inputs) ---")
            for u_in in res['user_inputs']:
                print(f"[Step {u_in['step_index']}] {u_in['content'].strip()}")

if __name__ == "__main__":
    main()
