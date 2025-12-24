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
