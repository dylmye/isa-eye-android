# Pre-launch report Robo script

`robo-script.json` drives the Play Console pre-launch report through the app's
core journey before the normal crawl continues:

1. add a Cash ISA (Lloyds) on the Accounts tab;
2. record a £250 contribution;
3. set up a £50 monthly auto-contribution;
4. set a £1,000 goal on Insights;
5. open the first Help article.

The script asserts the account row, contribution, goal progress and Help article
render, and captures `insights_goal` and `help_article` screenshots.

## Uploading

1. Build and upload the GPE release bundle as usual.
2. In Play Console, open **Test and release > Testing > Pre-launch report > Settings**.
3. Under **Control how pre-launch report explores your app**, upload
   `robo-script.json`.
4. Save and upload a new bundle to run the report.

The script is read at crawl time, so it can be updated without changing the app.

## How it targets the UI

Robo runs against the release build's accessibility tree. Compose's default
semantics do not expose stable resource IDs, so the app sets
`Modifier.semantics { testTagsAsResourceId = true }` on the main window and on
each dialog window. Compose exposes each test tag as the accessibility node's
raw `viewIdResourceName` (for example `robo_add_account`), so the script matches
them with `resourceIdRegex` (`.*robo_add_account$`) rather than a
package-qualified `resourceId`. Text is only used for dropdown options and
rendered content.

Keep the tags stable when refactoring the screens:

| Tag | Screen |
| --- | --- |
| `robo_tab_accounts`, `robo_tab_insights`, `robo_tab_help` | Navigation suite items |
| `robo_add_account` | Add-account FAB |
| `robo_add_nickname`, `robo_add_bank`, `robo_add_type` | Add-account fields |
| `robo_bank_search`, `robo_bank_list`, `robo_bank_<providerId>` | Provider picker sheet |
| `robo_add_continue`, `robo_add_save` | Add-account actions |
| `robo_accounts_list` | Accounts list |
| `robo_account_detail_grid` | Account detail grid |
| `robo_update_contributions`, `robo_setup_auto_contribution` | Account actions |
| `robo_contribution_amount`, `robo_contribution_save` | Contributions dialog |
| `robo_auto_amount`, `robo_auto_save` | Auto-contribution dialog |
| `robo_insights_grid`, `robo_set_goal` | Insights screen |
| `robo_goal_amount`, `robo_goal_save` | Goal dialog |
| `robo_help_list`, `robo_help_topic_<topicId>` | Help screen |

## Maintenance

- The goal and auto-contribution flows need a ruleset covering today's date. The
  seed data currently ends at 2026/2027, so add the next ruleset before the
  report is run after 5 April 2027.
- The script uses the GPE free tier's single account, so it stays within the
  four-account limit.
- `DISABLE_KEYBOARD` keeps the soft keyboard from covering the pinned action
  buttons. `VIEW_TEXT_CHANGED` still writes directly through the accessibility
  action.
