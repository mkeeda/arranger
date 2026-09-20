
def on_post_page(output, page, config):
    parts = [p for p in page.url.strip('/').split('/') if p]
    prefix = '../' * len(parts)
    rel_api = f'{prefix}api/'
    return output.replace('href="https://mkeeda.github.io/arranger/api/"', f'href="{rel_api}"')
