"""Fresh accessibility snapshots via the project's one-shot test instrumentation."""
import base64
import re
import subprocess
import xml.etree.ElementTree as ET

def snapshot(adb):
    output=subprocess.check_output(adb+['shell','am','instrument','-w','io.github.sunisup666.tiktools.probe/.InspectInstrumentation'],timeout=30)
    match=re.search(rb'INSTRUMENTATION_RESULT: xml_b64=([^\r\n]+)',output)
    if not match:raise RuntimeError(output.decode(errors='replace')[:1000])
    raw=base64.b64decode(match[1],validate=True)
    tree=ET.fromstring(raw)
    if not list(tree.iter('node')):raise RuntimeError('Empty active-window snapshot')
    return raw
