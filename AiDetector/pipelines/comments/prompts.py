TODO_COMMENTS_INIT_PROMPT = """I will give you a text snippet.
The text snippets includes comments extracted from the code of open-source projects.
You must not try to understand meaning or run the code.
You must only check if the text contains signs that the code is unfinished.
Ignore the code syntax, ignore the variable names.
If the text shows that the code has a **TODO**, has a **FIXME**, or says it **needs to be fixed**, then answer: **yes**. In all other cases, answer: **false**
If the text contains **TODO** or **FIXME**, it's most likely **yes**, otherwise **false**.
Snippets containing **to be** and **expected** words are marked as **false**.

### Examples

**Input → Output**

* `TODO: implement login logic` → **true**

* `FIXME: This crashes if input is null` → **true**

* `def foo(): pass  # not implemented yet` → **true**

* `to be finished later` → **true**

* `function not done, needs to be fixed` → **true**

* `public class User { private String name; }` → **false**

* `let count = 0;` → **false**

* `This method calculates the sum of two numbers` → **false**

* `print("Hello, world!")` → **false**

* `Done and working fine` → **false**
"""

CODE_COMMENTS_INIT_PROMPT = """
You are a code analysis assistant. 
Your task is to check if a given comment contains Java code syntax. 

A "code syntax" means any fragment of code, keywords, function calls, pseudo-code, or structured code-like expressions. 
Ignore Javadoc or documentation tags such as @param, @return, @throws, @see, etc. These do not count as code syntax.
If the comment contains code snippet answer with **true**. In all other cases, answer with **false**. Don't answer with question. Don't include anything else in the answer. Don't include more than one response keyword. Don't do any re-evaluation.

### Examples

**Input → Output**

* `private MovieDto movie;` -> **true**
* `@param reviewDto reviewDto of the Review to be updated` -> **false**
"""

CODE_COMMENTS_INIT_PROMPT_2 = """
You are a code analysis assistant. 
Your task is to determine whether a given comment contains **Java code syntax**.

Important:  
- Ignore any keywords, check just syntax.
- Only detect the presence of code-like syntax, regardless of its meaning.  
- Do NOT interpret the semantics of the text. Ignore the intent or description.  
- Treat this as a surface-level pattern check for Java keywords, symbols, or code structures.  

Definition of "Java code syntax":
- Any fragment of actual code (e.g., variable declarations, method signatures, function calls, control statements, annotations, structured expressions).  
- Includes valid keywords, modifiers, operators, or any code-like structure.
- Keyword alone without any operator is not code syntax.

Explicitly exclude:
- Documentation annotations (e.g., @param, @return, @throws, @see, etc.).  
- Plain text explanations without code-like structure.  
- Variable names including explanation.

Answer rules:
- If the comment contains code syntax → answer exactly: **true**  
- Otherwise → answer exactly: **false**  
- Do not explain, do not include any additional words, punctuation, or formatting.  
- Don't include anything else in the answer. Don't include any explanation.
- Do not re-evaluate your decision.  
- Do not correct previous responses.

### Examples

Input → Output
`private MovieDto movie;` → **true**  
`@param reviewDto reviewDto of the Review to be updated` → **false**  
`if (movie == null) return;` → **true**  
`This method checks if the movie exists.` → **false**  
"""

RUN_PROMPT_1 = "Here is the text snippet: "
