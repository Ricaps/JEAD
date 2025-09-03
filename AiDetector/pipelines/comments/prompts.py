__INIT_PROMPT = """I will give you a piece of text. You must check it and answer with one of these numbers:

* **0** → if the text contains code syntax and is not piece of JavaDoc.
* **1** → The text has the word TODO or FIXME, or says the code is not done yet (like "not implemented", "to be finished").
* **2** → if the text is plain string.

Your answer must be in this format:

```
Indicator: <number>
```
Provide also short explanation.
"""

__INIT_PROMPT_2 = """I will give you a text snippet.
The text snippets includes comments extracted from the code of open-source projects.
You must not try to understand meaning or run the code.
You must only check if the text contains signs that the code is unfinished.
Ignore the code syntax, ignore the variable names.
If the text shows that the code has a **TODO**, has a **FIXME**, or says it **needs to be fixed**, then answer: **yes**. In all other cases, answer: **no**
If the text contains **TODO** or **FIXME**, it's most likely **yes**, otherwise **no**.
Snippets containing **to be** and **expected** words are marked as **no**.

### Examples

**Input → Output**

* `TODO: implement login logic` → **yes**

* `FIXME: This crashes if input is null` → **yes**

* `def foo(): pass  # not implemented yet` → **yes**

* `to be finished later` → **yes**

* `function not done, needs to be fixed` → **yes**

* `public class User { private String name; }` → **no**

* `let count = 0;` → **no**

* `This method calculates the sum of two numbers` → **no**

* `print("Hello, world!")` → **no**

* `Done and working fine` → **no**
"""