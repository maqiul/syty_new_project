import re, os
root = r'I:\syty\syty-server\src\main\java\com\syty\controller'
for fn in os.listdir(root):
    if not fn.endswith('.java'): continue
    fp = os.path.join(root, fn)
    with open(fp, 'r', encoding='utf-8') as f:
        c = f.read()
    c = re.sub(r'import cn\.dev33\.satoken\.annotation\.SaCheckRole;\r?\n', '', c)
    c = re.sub(r'    @SaCheckRole\(\{[^}]*\}\)\n', '', c)
    with open(fp, 'w', encoding='utf-8') as f:
        f.write(c)
print('done')
