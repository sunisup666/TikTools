"""Check complete translations, format arguments and UI resource references offline."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'app/src/main/res'

def strings(directory):
    nodes=list(ET.parse(RES/directory/'strings.xml').getroot())
    result={node.attrib['name']:node.text or '' for node in nodes}
    assert len(result)==len(nodes),'Duplicate string resource names'
    return result

def main():
    english=strings('values');chinese=strings('values-zh')
    assert english.keys()==chinese.keys(),'Translation key sets differ'
    pattern=re.compile(r'%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])')
    def arguments(value):
        return sorted((index,kind) for index,kind in pattern.findall(value) if kind!='%')
    for name,value in english.items():
        assert arguments(value)==arguments(chinese[name]),('Format arguments differ',name)
        assert name=='language_chinese' or not re.search(r'[\u4e00-\u9fff]',value),('Untranslated English resource',name)
    java=ROOT/'app/src/main/java/io/github/sunisup666/tiktools'
    for source in java.glob('*.java'):
        text=source.read_text(encoding='utf-8')
        for name in re.findall(r'(?<!android\.)R\.string\.([a-zA-Z0-9_]+)',text):
            assert name in english,('Unknown resource',source.name,name)
        if source.name in {'MainActivity.java','MapActivity.java'}:
            for literal in re.findall(r'"(?:\\.|[^"\\])*"',text):
                assert not re.search(r'[\u4e00-\u9fff]',json.loads(literal)),('Hardcoded UI text',source.name,literal)
    print(f'Localization checks passed: {len(english)} matching English/Chinese resources, format arguments and UI references.')

if __name__=='__main__':main()
