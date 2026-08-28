from collections import Counter
from pathlib import Path
import re
from zipfile import ZipFile

from lxml import etree


SOURCE = Path(r"A:\4928bfc9-c8a1-484f-848e-9dcb9f15ba7a (1).docx")
FINAL = Path(
    r"E:\GuideStick\output\documents\视觉感知与云端大模型智能导盲辅助系统-官方模板最终版.docx"
)
TITLE = "视觉感知与云端大模型智能导盲辅助系统"
W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W}

EDITABLE_PARTS = {
    "word/document.xml",
    "word/settings.xml",
    "docProps/core.xml",
    "word/header3.xml",
    "word/header4.xml",
    "word/header5.xml",
    "word/header6.xml",
    "word/header7.xml",
}
FORBIDDEN_TEXT = (
    "[作品名称]",
    "填写说明",
    "提交时请删除本页",
    "二级标题",
    "三级标题",
    "图样例",
    "表样例",
    "云计算模式",
    "设计作品名称",
    "智途——融合视觉感知",
    "ESP32-CAM",
)


def main():
    failures = []
    with ZipFile(SOURCE) as source, ZipFile(FINAL) as final:
        source_names = set(source.namelist())
        final_names = set(final.namelist())
        if source_names != final_names:
            failures.append("DOCX package part set changed")

        for name in sorted(source_names & final_names):
            if name not in EDITABLE_PARTS and source.read(name) != final.read(name):
                failures.append(f"Preserve-only part changed: {name}")

        bad_zip_part = final.testzip()
        if bad_zip_part:
            failures.append(f"Corrupt ZIP member: {bad_zip_part}")

        document = etree.fromstring(final.read("word/document.xml"))
        full_text = "".join(document.xpath(".//w:t/text()", namespaces=NS))
        for marker in FORBIDDEN_TEXT:
            if marker in full_text:
                failures.append(f"Forbidden marker remains: {marker}")

        if TITLE not in full_text:
            failures.append("Final title missing from document body")

        paragraphs = [
            "".join(paragraph.xpath(".//w:t/text()", namespaces=NS)).strip()
            for paragraph in document.xpath(".//w:p", namespaces=NS)
        ]
        reference_heading_indexes = [
            index for index, text in enumerate(paragraphs) if text == "参考文献"
        ]
        if not reference_heading_indexes:
            failures.append("Reference heading missing")
        else:
            reference_start = reference_heading_indexes[-1]
            body_text = "".join(paragraphs[:reference_start])
            reference_texts = [
                text
                for text in paragraphs[reference_start + 1 :]
                if re.match(r"^\[\d+\]", text)
            ]
            reference_numbers = [
                int(re.match(r"^\[(\d+)\]", text).group(1))
                for text in reference_texts
            ]
            if reference_numbers != list(range(1, 13)):
                failures.append(
                    f"Expected sequential references [1]-[12], found {reference_numbers}"
                )

            cited_numbers = set()
            for citation in re.findall(
                r"\[(\d+(?:-\d+)?(?:,\d+(?:-\d+)?)*)\]", body_text
            ):
                for part in citation.split(","):
                    if "-" in part:
                        start, end = map(int, part.split("-", 1))
                        cited_numbers.update(range(start, end + 1))
                    else:
                        cited_numbers.add(int(part))
            missing_citations = sorted(set(range(1, 13)) - cited_numbers)
            if missing_citations:
                failures.append(
                    f"References not cited in body: {missing_citations}"
                )

        sections = document.xpath(".//w:sectPr", namespaces=NS)
        if len(sections) != 9:
            failures.append(f"Expected 9 sections, found {len(sections)}")

        styles = Counter(
            node.get(f"{{{W}}}val")
            for node in document.xpath(".//w:pPr/w:pStyle", namespaces=NS)
        )
        if styles["1"] != 7:
            failures.append(f"Expected 7 Heading 1 paragraphs, found {styles['1']}")
        if styles["2"] < 15 or styles["3"] < 15:
            failures.append("Insufficient level-2 or level-3 heading coverage")

        header_parts = [
            name
            for name in final_names
            if name.startswith("word/header") and name.endswith(".xml")
        ]
        if len(header_parts) != 8:
            failures.append(f"Expected 8 header parts, found {len(header_parts)}")
        for name in header_parts:
            header = etree.fromstring(final.read(name))
            header_text = "".join(header.xpath(".//w:t/text()", namespaces=NS))
            if "[作品名称]" in header_text:
                failures.append(f"Header placeholder remains: {name}")

        footer_parts = [
            name
            for name in final_names
            if name.startswith("word/footer") and name.endswith(".xml")
        ]
        if len(footer_parts) != 9:
            failures.append(f"Expected 9 footer parts, found {len(footer_parts)}")

        page_fields = 0
        for name in footer_parts:
            footer = etree.fromstring(final.read(name))
            instructions = footer.xpath(".//w:instrText/text()", namespaces=NS)
            page_fields += sum("PAGE" in instruction for instruction in instructions)
        if page_fields != 8:
            failures.append(f"Expected 8 page fields, found {page_fields}")

        settings = etree.fromstring(final.read("word/settings.xml"))
        update_fields = settings.find(f"{{{W}}}updateFields")
        if update_fields is None or update_fields.get(f"{{{W}}}val") != "true":
            failures.append("w:updateFields=true is missing")

        for name in final_names:
            if not name.startswith("word/") or not name.endswith(".xml"):
                continue
            xml = final.read(name)
            if b"0B6E69" in xml:
                failures.append(f"Previous teal palette remains in {name}")

    if failures:
        print("FAIL")
        for failure in failures:
            print(f"- {failure}")
        raise SystemExit(1)

    print("PASS")
    print(f"- Title length: {len(TITLE)}")
    print("- Package parts preserved")
    print("- 9 sections, 8 headers, 9 footers, 8 PAGE fields")
    print("- Heading levels 1/2/3 present")
    print("- 12 sequential references, all cited in body")
    print("- No template examples, old title, ESP32-CAM, or teal palette")


if __name__ == "__main__":
    main()
