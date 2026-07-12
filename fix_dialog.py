import re

with open('app/src/main/java/space/o4bit/projectasteria/ui/components/SettingsScreen.kt', 'r') as f:
    content = f.read()

# The dialog starts with `    if (showLanguageDialog) {` and ends before `\n}` of SettingsScreen
dialog_regex = r"    if \(showLanguageDialog\) \{.*?\n    \}\n"

match = re.search(dialog_regex, content, flags=re.DOTALL)
if match:
    dialog_code = match.group(0)
    # Remove from current location
    content = content.replace(dialog_code, "")
    
    # GeneralTabContent ends at:
    #         SectionCard {
    #             RichSettingsItem(
    #                 title = stringResource(R.string.report_issues),
    #                 subtitle = stringResource(R.string.help_improve),
    #                 action = {
    #                     Button(...)
    #                 }
    #             )
    #         }
    #     }
    # 
    #     if (showLanguageDialog) { ... } -> put it before the closing brace of GeneralTabContent.
    # The closing brace of GeneralTabContent is right after that SectionCard closing.
    
    # We will insert the dialog right before the closing brace of GeneralTabContent.
    # Let's find:
    target_str = """        SectionCard {
            RichSettingsItem(
                title = stringResource(R.string.report_issues),
                subtitle = stringResource(R.string.help_improve),
                action = {
                    Button(onClick = {
                        val githubIssueUrl = "https://github.com/O4bit/Project-Asteria/issues/new/choose"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.report))
                    }
                }
            )
        }
    }"""
    
    replacement = """        SectionCard {
            RichSettingsItem(
                title = stringResource(R.string.report_issues),
                subtitle = stringResource(R.string.help_improve),
                action = {
                    Button(onClick = {
                        val githubIssueUrl = "https://github.com/O4bit/Project-Asteria/issues/new/choose"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.report))
                    }
                }
            )
        }
    }
""" + dialog_code

    content = content.replace(target_str, replacement)
    
    with open('app/src/main/java/space/o4bit/projectasteria/ui/components/SettingsScreen.kt', 'w') as f:
        f.write(content)
    print("Fixed dialog position.")
else:
    print("Could not find dialog code.")
