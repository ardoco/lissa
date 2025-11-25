TEMPLATE = """
{
  "cache_dir": "./cache/<<DATASET>>",

  "gold_standard_configuration": {
    "path": "./datasets/req2req/<<DATASET>>/answer.csv",
    "hasHeader": "true"
  },

  "source_artifact_provider" : {
    "name" : "text",
    "args" : {
      "artifact_type" : "requirement",
      "path" : "./datasets/req2req/<<DATASET>>/high"
    }
  },
  "target_artifact_provider" : {
    "name" : "text",
    "args" : {
      "artifact_type" : "requirement",
      "path" : "./datasets/req2req/<<DATASET>>/low"
    }
  },
  "source_preprocessor" : {
    "name" : "artifact",
    "args" : {}
  },
  "target_preprocessor" : {
    "name" : "artifact",
    "args" : {}
  },
  "embedding_creator" : {
    "name" : "ollama",
    "args" : {
      "model": "qwen3-embedding:8b"
    }
  },
  "source_store" : {
    "name" : "custom",
    "args" : { }
  },
  "target_store" : {
    "name" : "custom",
    "args" : {
      "max_results" : "<<RETRIEVAL_COUNT>>"
    }
  },
  "classifier" : {
    "name" : "<<CLASSIFIER_MODE>>",
    "args" : {
      <<ARGS>>
    }
  },
  "result_aggregator" : {
    "name" : "any_connection",
    "args" : {}
  },
  "tracelinkid_postprocessor" : {
    "name" : "<<POSTPROCESSOR>>",
    "args" : {}
  }
}
"""

# Configurations
datasets = ["GANNT", "ModisDataset", "WARC", "dronology", "CM1-NASA"]
postprocessors = ["req2req", "identity", "req2req", "identity", "identity"]
retrieval_counts = [str(x) for x in [4, 4, 4, 4, 4]]

dir = "req2req-embedding"

for dataset, postprocessor, retrieval_count in zip(datasets, postprocessors, retrieval_counts):
    with open(f"./configs/{dir}/{dataset}_no_llm.json", "w") as f:
        f.write(TEMPLATE.replace("<<DATASET>>", dataset).replace("<<CLASSIFIER_MODE>>", "mock").replace("<<ARGS>>", "").replace("<<POSTPROCESSOR>>", postprocessor).replace("<<RETRIEVAL_COUNT>>", retrieval_count))
