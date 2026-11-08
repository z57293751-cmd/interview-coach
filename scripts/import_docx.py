# -*- coding: utf-8 -*-
"""按《AI 面试小站 · 设计方案》附录 B 规则，从 docx 导入题库并输出 SQL。

用法：
    pip install python-docx
    python scripts/import_docx.py "面试问题归纳.docx" > src/main/resources/db/seed_questions.sql

规则：
    1. 认 "Q " 开头段落为题目，紧跟的 "A " 段落为参考答案；
    2. 一级标题（一、项目类 / 二、Java 基础 / 三、数据库 / 四、框架）映射为 category；
    3. 含 ★ 的题 difficulty=3，其余默认 2；
    4. keyPoints 第一版留空 []；
    5. 导入后人工过一遍，删除重复项。
"""
import re
import sys

try:
    from docx import Document
except ImportError:
    print("缺少依赖，请先执行: pip install python-docx", file=sys.stderr)
    sys.exit(1)

HEADING_RE = re.compile(r'^(一|二|三|四)、')
HEADING_CATEGORY = {'一': '项目', '二': 'Java 基础', '三': '数据库', '四': '框架'}


def escape(value):
    return value.replace("\\", "\\\\").replace("'", "''")


def main(path):
    doc = Document(path)
    category = 'Java 基础'
    rows = []
    current = None

    for para in doc.paragraphs:
        text = para.text.strip()
        if not text:
            continue

        match = HEADING_RE.match(text)
        if match:
            category = HEADING_CATEGORY.get(match.group(1), category)
            continue

        if text.startswith('Q '):
            title = text[2:].strip()
            current = {
                'category': category,
                'title': title,
                'answer': '',
                'difficulty': 3 if '★' in title else 2,
            }
            rows.append(current)
        elif text.startswith('A ') and current is not None and not current['answer']:
            current['answer'] = text[2:].strip()

    print("USE coach;")
    print()
    print("INSERT INTO question (category, title, answer_standard, key_points, difficulty, source) VALUES")
    values = []
    for row in rows:
        if not row['answer']:
            continue
        values.append(
            "('{category}', '{title}', '{answer}', '[]', {difficulty}, '面试归纳')".format(
                category=escape(row['category']),
                title=escape(row['title']),
                answer=escape(row['answer']),
                difficulty=row['difficulty'],
            )
        )
    print(",\n".join(values) + ";")
    print()
    print("-- 共 %d 条，导入后请人工过一遍，删除明显重复的题。" % len([r for r in rows if r['answer']]))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python scripts/import_docx.py <你的docx文件>", file=sys.stderr)
        sys.exit(1)
    main(sys.argv[1])