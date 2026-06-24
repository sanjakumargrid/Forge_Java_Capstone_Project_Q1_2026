import os
import re

def update_api_paths():
    updated_files = []
    # Match /api/ followed by anything except v1/
    # We use a negative lookahead (?!v1/)
    pattern = re.compile(r'/api/(?!v1/)')
    
    for root, dirs, files in os.walk('.'):
        if '.git' in root or 'target' in root or 'node_modules' in root:
            continue
        for file in files:
            if file.endswith(('.java', '.yml', '.yaml', '.properties', '.sh', '.md')):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    new_content = pattern.sub('/api/v1/', content)
                    
                    if new_content != content:
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        updated_files.append(path)
                except Exception as e:
                    print(f"Error reading {path}: {e}")
                    
    for f in updated_files:
        print(f"Updated: {f}")

if __name__ == "__main__":
    update_api_paths()
