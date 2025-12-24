def split_camel_case(text: str) -> list[str]:
    result = []
    last_index = 0
    for index, char in enumerate(text):
        if char.isupper():
            if last_index + 1 == index and text[last_index - 1].isupper():
                __append_consecutive_capitals(char, result)

            __append_part(index, last_index, result, text)
            last_index = index

    input_length = len(text)
    if last_index != input_length - 1:
        current_part = text[last_index:]
        if len(current_part) == 0:
            return result
        result.append(current_part.lower())

    last_letter = text[input_length - 1]
    if last_letter.isupper():
        __append_consecutive_capitals(last_letter, result)

    return result


def __append_consecutive_capitals(char, result):
    char = char.lower()
    last_item_index = len(result) - 1
    result[last_item_index] = result[last_item_index] + char


def __append_part(index, last_index, result, text):
    current_part = text[last_index:index]

    if len(current_part) == 0:
        return
    result.append(current_part.lower())
