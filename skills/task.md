---
name: task
description: Quick task creation in GitHub Issues. Creates a task issue, executes work, and closes it upon completion.
---

You are creating and managing a development task.

# Workflow

When invoked with `/task [description]`:

1. **Create GitHub Issue**
```bash
gh issue create \
  --title "[TASK] [Brief Description]" \
  --body "**Description:**
[Detailed description]

**Acceptance Criteria:**
- [ ] [Criterion 1]
- [ ] [Criterion 2]

**Estimated Time:** [time]

---
> *Created by Claude Code*" \
  --label "task,ai-generated" \
  --assignee "@me"
```

2. **Execute Task**
   - Implement the required changes
   - Follow coding standards
   - Add tests if needed

3. **Commit Changes**
```bash
git commit -m "[type]: [description]" \
  -m "Refs: #[issue-number]" \
  -m "Co-authored-by: Claude Agent <claude@ai.bot>" \
  -m "X-Agent: @Dev"
```

4. **Close Issue**
```bash
gh issue close [issue-number] --comment "Task completed:
- [Summary of changes]
- [Important decisions made]

All acceptance criteria met ✓

Co-authored-by: Claude Agent <claude@ai.bot>"
```

# Important Rules

⚠️ **EVERY task MUST have an issue**
- Create issue BEFORE starting work
- Close issue AFTER completing work
- Never work without an issue

# Task Types

- **Feature Task:** New functionality
- **Bug Fix Task:** Fixing a defect
- **Refactoring Task:** Code improvement
- **Documentation Task:** Adding/updating docs
- **Technical Debt:** Addressing technical debt

# Example Usage

```
/task Add validation for email field in registration form
```

This will:
1. Create issue #42
2. Implement email validation
3. Add unit tests
4. Commit with proper format
5. Close issue #42 with summary
