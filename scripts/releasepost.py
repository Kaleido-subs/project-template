# Constants to fill out before you run this!
PROJECT_TITLE = "title"
PROJECT_ALIAS = "alias"
TWEET_HASHES = "#hash1 #hash2"


def _get_input() -> dict[str, str | int]:
    data = {}

    while not data.get("epnum"):
        try:
            data["epnum"] = int(input("Episode number: "))
        except ValueError:
            print("Invalid episode number. Please enter a valid number.")

    while True:
        data["nyaa"] = input("Nyaa url: (if applicable): ")
        data["nyaa_min"] = input("Nyaa (mini) url (if applicable): ")

        data["neko"] = input("Neko url: (if applicable): ")
        data["neko_min"] = input("Neko (mini) url (if applicable): ")

        if any(data[k] for k in ("nyaa", "neko", "nyaa_min", "neko_min")):
            break

        print("No URLs provided. Please enter at least one URL.")

    return data


def _get_release_command(data: dict[str, str | int]) -> str:
    raise NotImplementedError(
        "Not implemented yet. For now, always assume all urls are provided."
    )


def _get_tweet_content(data: dict[str, str | int]) -> str:
    raise NotImplementedError(
        "Not implemented yet. For now, always assume all urls are provided."
    )


# user input
data = _get_input()

print()
print()

# Nino release command
print(
    f"/release episode project:{PROJECT_ALIAS} episode:{data['epnum']} url:<{data['nyaa']}> |"
    f" {data['neko']} | -# Mini | -# <{data['nyaa_min']}> | -# <{data['neko_min']}>"
)

print()
print()
print()

# bsky tweet
print(
    f"""📰 {PROJECT_TITLE} - Episode {data["epnum"]}

{data["nyaa"]}
{data["neko"]}

(mini: {data["nyaa_min"]} / {data["neko_min"]})

{TWEET_HASHES}
"""
)
