# -*- coding: utf-8 -*-
"""AI 面试小站 · 接口冒烟测试

用法：
    python scripts/smoke_test.py                 # 快速检查（不花钱，不调 AI）
    python scripts/smoke_test.py --full          # 含 AI 评分链路（约 5 秒，花几分钱）

前提：应用已在 http://localhost:8080 运行。
如果站点开了访问口令，先设置环境变量：
    set COACH_PASSWORD=coach2026

全部通过时退出码为 0，有失败则为 1。
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request
from http.cookiejar import CookieJar

BASE = os.environ.get("COACH_BASE", "http://localhost:8080")
PASSWORD = os.environ.get("COACH_PASSWORD", "")

opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(CookieJar()))
results = []


def call(method, path, payload=None, timeout=120):
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(
        BASE + path, data=data, method=method,
        headers={"Content-Type": "application/json; charset=utf-8"})
    t0 = time.time()
    try:
        with opener.open(req, timeout=timeout) as r:
            body = json.loads(r.read().decode("utf-8"))
            return r.status, body, round(time.time() - t0, 2)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "ignore")
        try:
            body = json.loads(raw)
        except Exception:
            body = {"raw": raw[:200]}
        return e.code, body, round(time.time() - t0, 2)


def check(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("  [PASS] " if ok else "  [FAIL] ") + name + (("  -> " + detail) if detail else ""))
    return ok


def main():
    full = "--full" in sys.argv

    print("")
    print("AI 面试小站 · 冒烟测试")
    print("目标: " + BASE)
    print("")

    # 0. 登录（可选）
    if PASSWORD:
        code, body, _ = call("POST", "/api/login", {"password": PASSWORD})
        if not check("口令登录", code == 200 and body.get("code") == 0,
                     str(body.get("msg", ""))):
            print("\n口令不对或站点没开口令，检查 COACH_PASSWORD 环境变量")
            return finish()

    # 1. 健康检查
    code, body, ms = call("GET", "/api/ping")
    check("健康检查 /api/ping", code == 200 and body.get("code") == 0, f"{ms}s")

    # 2. 题库列表
    code, body, ms = call("GET", "/api/questions?page=1&size=5")
    total = (body.get("data") or {}).get("total", 0)
    check("题库列表 /api/questions", code == 200 and total > 0, f"{total} 道题, {ms}s")

    # 3. 题目详情
    code, body, ms = call("GET", "/api/questions/1")
    q = body.get("data") or {}
    check("题目详情 /api/questions/1", code == 200 and bool(q.get("title")), q.get("title", "")[:30])

    # 4. 复盘列表
    code, body, _ = call("GET", "/api/review?mastered=0")
    check("复盘列表 /api/review", code == 200 and body.get("code") == 0)

    if not full:
        print("\n（跳过 AI 链路，加 --full 可测评分和模拟面试）")
        return finish()

    # 5. 提交回答 + AI 评分
    answer = ("索引用 B+ 树，是因为非叶子节点只存键不存数据，同样大小的页能放更多键，"
              "扇出更大、树更矮，查一次走的磁盘 IO 更少；叶子节点还用链表连起来，范围查询方便。")
    code, body, ms = call("POST", "/api/attempts", {"questionId": 2, "userAnswer": answer})
    aid = (body.get("data") or {}).get("attemptId")
    check("提交回答 /api/attempts", code == 200 and aid is not None, f"attemptId={aid}")

    if aid:
        code, body, ms = call("POST", f"/api/attempts/{aid}/evaluate")
        d = body.get("data") or {}
        score = d.get("score")
        check("AI 评分 /evaluate", code == 200 and isinstance(score, int) and 0 <= score <= 100,
              f"{score} 分, {ms}s")
        check("评分有点评", bool(d.get("aiComment")), (d.get("aiComment") or "")[:30])

    # 6. 模拟面试：有简历才测
    code, body, _ = call("GET", "/api/resumes")
    resumes = body.get("data") or []
    if resumes:
        rid = resumes[0].get("id")
        code, body, ms = call("POST", "/api/mock/sessions", {"resumeId": rid})
        d = body.get("data") or {}
        check("模拟面试开场 /api/mock/sessions",
              code == 200 and d.get("total", 0) >= 5, f"{d.get('total')} 题, {ms}s")
    else:
        print("  [SKIP] 模拟面试：还没有上传过简历")

    return finish()


def finish():
    total = len(results)
    passed = sum(1 for _, ok, _ in results if ok)
    print("")
    print(f"结果: {passed}/{total} 通过")
    print("PASS" if passed == total and total > 0 else "FAIL")
    print("")
    return 0 if passed == total and total > 0 else 1


if __name__ == "__main__":
    sys.exit(main())
