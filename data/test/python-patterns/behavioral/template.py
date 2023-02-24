def get_text() -> str:
    return "plain-text"


def get_pdf() -> str:
    return "pdf"


def get_csv() -> str:
    return "csv"


def convert_to_text(data: str) -> str:
    print("[CONVERT]")
    return f"{data} as text"


def saver() -> None:
    print("[SAVE]")


def template_function(getter, converter=False, to_save=False) -> None:
    data = getter()
    print(f"Got `{data}`")

    if len(data) <= 3 and converter:
        data = converter(data)
    else:
        print("Skip conversion")

    if to_save:
        saver()

    print(f"`{data}` was processed")


def main():
    template_function(get_text, to_save=True)
    template_function(get_pdf, converter=convert_to_text)
    template_function(get_csv, to_save=True)

if __name__ == "__main__":
    main()