---
name: pdf-and-docs
description: Read long PDFs, scanned documents and large text files that the user attached or put in the workspace, page by page. Use when the user asks about a PDF, a document, a report, a scanned page or a very large file.
---
# PDFs, scans and large documents

## How the app gives you documents
- **Text PDFs** arrive as extracted text, page by page (`--- page N ---`). No page limit; long
  documents are paged, so ask for the next page if you need it.
- **Scanned/image PDFs** arrive as page images for a vision model. Read them like a scan:
  handle rotated pages, stamps and handwriting by saying what you can actually see.
- **Large text files** are never truncated silently: `read` reports the byte count and the
  characters shown, and accepts `offset`/`limit` to page through.

## How to work
1. Answer the question from the part of the document that contains it; do not summarise the
   whole file when the user asked about one clause.
2. Quote the page number and the sentence you used. If a page was unreadable, say which.
3. For a long document, produce a short outline first (page ranges), then go deep.
4. Compare documents with `grep` for a key term across the folder instead of reading both fully.

## Rules
- Never invent text on a page that is blurred or missing.
- Numbers, dates and amounts: copy them exactly, do not round.
- If the user needs the text itself, write it to a file with `write_file` and say the path.

بالعربية: اقتبس رقم الصفحة والنص اللي استندت عليه، وقول بصراحة لو صفحة مش واضحة.
