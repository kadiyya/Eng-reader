import json, re, sys
SRC = sys.argv[1]
OUT = sys.argv[2]

def load(name):
    raw = open(f"{SRC}/{name}", encoding="utf-8", errors="replace").read()
    return raw.replace("\r\n", "\n")

def paragraphs(raw, poem):
    out = []
    for p in re.split(r"\n\s*\n", raw):
        if poem:
            lines = [re.sub(r"\s+", " ", l).strip() for l in p.split("\n")]
            p = "\n".join(l for l in lines if l)
        else:
            p = re.sub(r"\s+", " ", p).strip()
        if p:
            out.append(p)
    return out

SENT_END = re.compile(r'[.!?]["\'”’)]*(?=\s|$)')

def clean(t):
    t = t.replace("_", "").replace("--", "—").replace(" ", " ")
    t = re.sub(r"\[Illustration[^\]]*\]", "", t)
    return re.sub(r"[ \t]+", " ", t).strip()

def is_heading(p):
    letters = [c for c in p if c.isalpha()]
    return bool(letters) and all(c.isupper() for c in letters) and len(p.split()) < 15

def extract(book, start, words, poem=False, occ=1, fix=()):
    paras = paragraphs(load(book), poem)
    flat = [p.replace("\n", " ") for p in paras]
    hits = [i for i, p in enumerate(flat) if start in p]
    if len(hits) < occ:
        raise SystemExit(f"NOT FOUND: {book}: {start!r}")
    i = hits[occ - 1]
    first = paras[i]
    # locate start inside the (possibly multi-line) first paragraph
    idx = first.replace("\n", " ").index(start)
    chunks, n = [first[idx:]], 0
    n = len(chunks[0].split())
    i += 1
    while n < words and i < len(paras):
        if is_heading(paras[i]):
            break
        chunks.append(paras[i]); n += len(paras[i].split()); i += 1
    text = "\n\n".join(chunks)
    if not poem:
        # trim at the first sentence end after the word target
        over = n - words
        last_len = len(chunks[-1].split())
        if over > 0 and len(chunks) > 1 and n - last_len >= words * 0.8:
            chunks.pop()
        elif over > 0:
            last = chunks[-1]
            keep_words = len(last.split()) - over
            cut_from = len(" ".join(last.split()[:max(keep_words, 1)]))
            m = SENT_END.search(last, cut_from)
            if m and m.end() < len(last):
                chunks[-1] = last[:m.end()]
        text = "\n\n".join(chunks)
    text = clean(text)
    for a, b in fix:
        text = text.replace(a, b, 1)
    return text

exec(open(sys.argv[0].replace("gen.py", "spec.py"), encoding="utf-8").read())

out = []
num = 0
for w in WORLDS:
    levels = []
    for lv in w["levels"]:
        num += 1
        text = extract(**lv["src"])
        levels.append({"number": num, "title": lv["title"], "wpm": lv["wpm"],
                       "boss": lv.get("boss", False), "text": text})
        print(f"#{num:02d} {w['name'][:18]:18} {lv['title'][:30]:30} {lv['wpm']} wpm {len(text.split())} words", file=sys.stderr)
    out.append({k: w[k] for k in ("name", "book", "author", "color", "emoji")} | {"levels": levels})
json.dump(out, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
