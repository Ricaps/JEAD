import torch
from datasets import DatasetDict, Column, Dataset
from transformers import AutoTokenizer, AutoModelForSequenceClassification, DataCollatorWithPadding, TrainingArguments, \
    Trainer
import evaluate
import numpy as np
import logging

CODEBERT_BASE = "microsoft/codebert-base"
EVAL_METRICS = evaluate.combine(["accuracy", "f1", "precision", "recall"])


class CommentsTrainer:
    def __init__(self, output_dir: str, classes: list[str], special_tokens: list[str]):
        self.output_dir = output_dir
        self.classes = classes
        self.class2id = self.__get_class2id(classes)
        self.id2class = self.__get_id2class(classes)
        self.special_tokens = special_tokens
        self.__logger = logging.getLogger(self.__class__.__name__)

    def train_model(self, dataset: DatasetDict):
        self.__logger.info("Initializing tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(CODEBERT_BASE, additional_special_tokens=self.get_special_tokens())

        self.__logger.info("Preprocessing dataset...")
        tokenized_dataset = dataset.map(lambda element: self.__preprocess(tokenizer, element))
        data_collator = DataCollatorWithPadding(tokenizer=tokenizer)

        training_arguments = self.__build_arguments()

        model = AutoModelForSequenceClassification.from_pretrained(
            CODEBERT_BASE,
            num_labels=len(self.classes),
            id2label=self.id2class,
            label2id=self.class2id,
            problem_type="multi_label_classification"
        )

        model.resize_token_embeddings(len(tokenizer))

        trainer = Trainer(
            model=model,
            args=training_arguments,
            train_dataset=tokenized_dataset["train"],
            eval_dataset=tokenized_dataset["test"],
            data_collator=data_collator,
            compute_metrics=self.__eval_fnc
        )

        self.__logger.info("Training...")
        trainer.train()

        trainer.save_model(self.output_dir)
        tokenizer.save_pretrained(self.output_dir)

    @staticmethod
    def evaluate(model_path: str, dataset: Dataset) -> dict[str, float]:
        tokenizer = AutoTokenizer.from_pretrained(model_path)

        def preprocess(examples):
            return tokenizer(examples["text"], truncation=True, padding="max_length")

        dataset = dataset.map(preprocess)

        model = AutoModelForSequenceClassification.from_pretrained(
            model_path,
            problem_type="multi_label_classification"
        )

        trainer = Trainer(
            model=model,
            tokenizer=tokenizer,
            compute_metrics=CommentsTrainer.__eval_fnc,
        )

        return trainer.evaluate(dataset)

    def __preprocess(self, tokenizer, element: Column):
        comment_type: str = element["commentType"]
        text = f"[{comment_type.upper()}] {element["text"]}"

        labels = torch.zeros(len(self.classes), dtype=torch.float)
        element_labels = element["labels"]

        for label in element_labels:
            label_id = self.class2id[label]
            labels[label_id] = 1.

        tokenized = tokenizer(text, truncation=True)
        tokenized["labels"] = labels

        return tokenized

    def __build_arguments(self):
        args = TrainingArguments(
            output_dir=self.output_dir,
            save_strategy="epoch",
            eval_strategy="epoch",
            num_train_epochs=3,
            per_device_train_batch_size=3,
            per_device_eval_batch_size=3,
            weight_decay=0.01,
            load_best_model_at_end=True
        )

        return args

    @staticmethod
    def __get_sigmoid(prediction):
        return 1 / (1 + np.exp(-prediction))

    @staticmethod
    def __eval_fnc(prediction):
        predictions, labels = prediction
        predictions = CommentsTrainer.__get_sigmoid(predictions)
        binary_predictions = (predictions > 0.5).astype(int).reshape(-1)

        return EVAL_METRICS.compute(predictions=binary_predictions, references=labels.astype(int).reshape(-1))

    @staticmethod
    def __get_class2id(classes: list[str]):
        return {class_: index for index, class_ in enumerate(classes)}

    @staticmethod
    def __get_id2class(classes: list[str]):
        return {index: class_ for index, class_ in enumerate(classes)}

    def get_special_tokens(self) -> list[str]:
        return list(map(lambda el: f"[{el}]", self.special_tokens))
