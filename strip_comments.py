import os
import re

def remove_comments_from_file(filepath):
    with open(filepath, 'r') as f:
        text = f.read()

    pattern = re.compile(
        r'(?P<raw_string>\"\"\"(?:.|\n)*?\"\"\")|(?P<string>"(?:\\.|[^"\\])*")|(?P<char>\'(?:\\.|[^\'\\])*\')|(?P<block_comment>/\*.*?\*/)|(?P<line_comment>//.*?$)',
        re.DOTALL | re.MULTILINE
    )

    def replacer(match):
        if match.group('raw_string') is not None:
            return match.group('raw_string')
        elif match.group('string') is not None:
            return match.group('string')
        elif match.group('char') is not None:
            return match.group('char')
        else:
            return '' # Remove comments

    cleaned_text = pattern.sub(replacer, text)
    
    # Remove blank lines that only have whitespaces (to clean up spaces left by stripped comments)
    cleaned_text = "\n".join([line for line in cleaned_text.split('\n') if line.strip() != ""])

    with open(filepath, 'w') as f:
        f.write(cleaned_text)

for root, dirs, files in os.walk('app/src/main/java/space/o4bit/projectasteria/'):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            remove_comments_from_file(filepath)
