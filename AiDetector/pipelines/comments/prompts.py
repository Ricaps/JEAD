TODO_COMMENTS_INIT_PROMPT = """I will give you a text snippet.
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

CODE_COMMENTS_INIT_PROMPT = """
You are a code analysis assistant. 
Your task is to check if a given comment contains code snippets. 

- A "code snippet" means any fragment of code, keywords, function calls, pseudo-code, or structured code-like expressions. 
- Ignore Javadoc or documentation tags such as @param, @return, @throws, @see, etc. These do not count as code snippets.
- Respond only with "yes" if the comment contains code snippets, or "no" if it does not.

"""

RUN_PROMPT_1 = "Here is the text snippet: "