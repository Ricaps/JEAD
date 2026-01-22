def input_until_integer(input_str: str) -> int:
    while True:
        value = input(input_str).strip()
        if value.startswith("+"):
            if value[1:].isdigit():
                return int(value)
        elif value.startswith("-"):
            if value[1:].isdigit():
                return -int(value)
        elif value.isdigit():
            return int(value)
