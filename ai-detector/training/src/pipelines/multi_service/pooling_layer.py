import torch


def mean_pooling(model_output, attention_mask):
    """https://zilliz.com/ai-faq/how-do-i-implement-embedding-pooling-strategies-mean-max-cls"""
    token_embeddings = model_output["last_hidden_state"]
    input_mask_expanded = (
        attention_mask.unsqueeze(-1).expand(token_embeddings.size()).float()
    )
    # masked mean
    sum_embeddings = torch.sum(token_embeddings * input_mask_expanded, dim=1)
    sum_mask = torch.clamp(input_mask_expanded.sum(dim=1), min=1e-9)
    return sum_embeddings / sum_mask


def cls_pooling(model_output):
    return model_output["last_hidden_state"][:, 0]


def max_pooling(model_output, attention_mask):
    token_embeddings = model_output["last_hidden_state"]
    input_mask_expanded = (
        attention_mask.unsqueeze(-1).expand(token_embeddings.size()).float()
    )
    token_embeddings[
        input_mask_expanded == 0
    ] = -1e9  # Set padding to large negative value
    return torch.max(token_embeddings, dim=1)[0]
