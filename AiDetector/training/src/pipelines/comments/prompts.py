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
- Code keyword doesn't mean it's a code syntax.
- Only detect the presence of code-like syntax, regardless of its meaning.  
- Treat this as a surface-level pattern check for Java keywords, symbols, or code structures.  

Definition of "Java code syntax":
- Any fragment of actual code (e.g., variable declarations, method signatures, function calls, control statements, annotations, structured expressions).  
- Includes valid modifiers, operators, or any code-like structure.
- Only keyword itself is not code syntax.

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

GENERATE_VARIATIONS_TODO = """I want you to help me with generating similar snippets based on the input.
You are helping me with oversampling code comments dataset. 
You must generate 5 snippets. 
The snippet must not be longer than 200 characters. 
The input I provide to you is code comment extracted from the code base. 
Rephrase it, change keywords, but keep the meaning of the comment (TODO comments and style, ...).
The meaning of the comment has to be that something is not yet finished, undone, or should be fixed.
Don't include any comment syntax characters like slashes, stars, ...
You can change name of classes, variables, if present.
Return the snippets as array of string within the provided format."""

GENERATE_VARIATIONS_TODO_2 = """
Generate 5 unique, paraphrased variants of a provided code comment, each indicating that the code is unfinished, incomplete, or requires fixing, following these requirements:
- Goal: Create a diverse dataset of code comments suited for training purposes.
- Input: A code comment string that clearly communicates an undone, incomplete, or fix-needed state.
Each generated snippet must:
- Clearly express that something is unfinished or requires attention/fixing.
- Not exceed 200 characters in length.
- Omit all forms of comment syntax.
- Be rewritten in a format that conveys a TODO or unfinished state, using phrasing distinct from the original input.
- Use varied keywords and sentence structures while preserving the original meaning.
- Optionally rename any classes, functions, or variables present to introduce more variation.
- In a minority of cases, you may use markers such as FIXME, UNDONE, TEMPORARY, TEMP, etc., to indicate incompleteness; in other minority cases, express the undone state directly in the sentence without any TODO-like marker.
- Ensure all snippets are distinct from one another.
Confirm that all paraphrases:
- Convey an undone, incomplete, or needs-fixing state without repeating words or structure from the input.
- Adhere to the 200 character maximum.
- Do not include any comment delimiters or syntax.
After generating snippets, validate that all requirements are met. If any constraint is not satisfied, self-correct and output a revised JSON array of 5 valid strings.
Output Format:
- Provide a JSON array of exactly 5 strings, each representing a unique paraphrased code comment. No explanatory text, formatting, or extra markup may be included.
- If the input does not clearly reference an unfinished, incomplete, or needs-fixing state, return an empty JSON array: [].
- All output must be strictly valid JSON.
Refer to the examples provided for guidance on acceptable input and output patterns."""

GENERATE_VARIATIONS_CODE = """I want you to help me with generating similar snippets based on the input.
You are helping me with oversampling code comments dataset. 
You must generate 5 snippets. 
The snippet must not be longer than 200 characters. 
The input I provide to you is code comment extracted from the code base. 
The snippet includes commented-out out.
I want you to generate similar code based on the input, with a bit different structure and namings.
Don't include any comment syntax characters like slashes, stars, ...
Return the snippets as array of string within the provided format."""
