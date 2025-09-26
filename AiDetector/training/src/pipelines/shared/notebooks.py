def comma_separated_paths() -> list[str]:
    """
    Asks user for comma separated paths input.

    :return: list of paths as strings
    """
    input_paths = input("Insert comma separated input paths: ").split(",")
    return list(map(lambda input_path: input_path.strip(), input_paths))
