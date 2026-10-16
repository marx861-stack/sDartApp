import sys
with open(sys.argv[1], 'rb') as f:
    data = f.read()

import re
strings = re.findall(b'[a-zA-Z0-9_]{4,}', data)
found_classes = set()
for s in strings:
    s_str = s.decode('ascii', errors='ignore').lower()
    if 'dart' in s_str or 'spitze' in s_str or 'board' in s_str:
        found_classes.add(s_str)

print("Found relevant strings in model:")
for cls in found_classes:
    print(cls)
