TEMPLATE_D2M = """
{
  "cache_dir": "./cache-d2m/<<PROJECT>>-<<SEED>>",
  "gold_standard_configuration": {
    "path": "<<GS_PATH>>",
    "hasHeader": "true",
    "swap_columns": "<<SWAP_COLUMNS>>"
  },

  "source_artifact_provider" : {
    "name" : "text",
    "args" : {
      "artifact_type" : "software architecture documentation",
      "path" : "<<TEXT_PATH>>"
    }
  },
  "target_artifact_provider" : {
    "name" : "text",
    "args" : {
      "artifact_type" : "software architecture model",
      "path" : "<<UML_PATH>>"
    }
  },
  "source_preprocessor" : {
    "name" : "sentence",
    "args" : { }
  },
  "target_preprocessor" : {
    "name" : "model_uml",
    "args" : {
      "includeUsages" : false,
      "includeOperations" : false,
      "includeInterfaceRealizations" : false
    }
  },
  "embedding_creator" : {
    "name" : "openai",
    "args" : {
      "model": "text-embedding-3-large"
    }
  },
  "source_store" : {
    "name" : "custom",
    "args" : { }
  },
  "target_store" : {
    "name" : "custom",
    "args" : {
      "max_results" : "10"
    }
  },
  "classifier" : {
    "name" : "reasoning_openai",
    "args" : {
      "model" : "<<MODEL>>",
      "seed": "<<SEED>>"
    }
  },
  "result_aggregator" : {
    "name" : "any_connection",
    "args" : {
      "source_granularity" : "1",
      "target_granularity" : "1"
    }
  },
  "tracelinkid_postprocessor" : {
    "name" : "sad2sam",
    "args" : { }
  }
}
"""

from typing import List, Tuple

def swap(definition: str, pairs: List[Tuple[str, str]]):
    for pair in pairs:
        definition = definition.replace(pair[0], "~~TEMP~~").replace(pair[1], pair[0]).replace("~~TEMP~~", pair[1])
    return definition

TEMPLATE_M2D = swap(TEMPLATE_D2M, [("target_artifact_provider", "source_artifact_provider"), ("target_preprocessor", "source_preprocessor"), ("sad2sam", "sam2sad")])

projects = ["mediastore", "teastore", "teammates", "jabref", "bigbluebutton"]
uml_paths = ["model_2016/uml/ms.uml", "model_2020/uml/teastore.uml", "model_2021/uml/teammates.uml", "model_2021/uml/jabref.uml", "model_2021/uml/bbb.uml"]
text_paths = ["text_2016/mediastore.txt", "text_2020/teastore.txt", "text_2021/teammates.txt", "text_2021/jabref.txt", "text_2021/bigbluebutton_1SentPerLine.txt"]
goldstandard_paths = ["goldstandards/goldstandard-mediastore.csv", "goldstandards/goldstandard-teastore.csv", "goldstandards/goldstandard-teammates.csv", "goldstandards/goldstandard-jabref.csv", "goldstandards/goldstandard-bigbluebutton.csv"]

# Configurations
seeds = ["133742243"]
models = ["gpt-4o-mini-2024-07-18", "gpt-4o-2024-05-13"]


import os
for project, uml, text, gs in zip(projects, uml_paths, text_paths, goldstandard_paths):
    gs_path = f"./datasets/doc2model/{project}/{gs}"
    uml_path = f"./datasets/doc2model/{project}/{uml}"
    text_path = f"./datasets/doc2model/{project}/{text}"

    template_d2m = TEMPLATE_D2M.replace("<<GS_PATH>>", gs_path).replace("<<UML_PATH>>", uml_path).replace("<<TEXT_PATH>>", text_path).replace("<<PROJECT>>", project)
    template_m2d = TEMPLATE_M2D.replace("<<GS_PATH>>", gs_path).replace("<<UML_PATH>>", uml_path).replace("<<TEXT_PATH>>", text_path).replace("<<PROJECT>>", project)

    for seed in seeds:
        os.makedirs("./configs/doc2model", exist_ok=True)
        os.makedirs("./configs/doc2model", exist_ok=True)
        # Generate
        for prompting in ["reasoning_openai", "simple_openai"]:
          if prompting != "reasoning_openai":
              template_d2m = template_d2m.replace("reasoning_openai", prompting)
              template_m2d = template_m2d.replace("reasoning_openai", prompting)
          for model in models:
              # Only Name
              with open(f"./configs/doc2model/{project}_d2m_{seed}_{model}_{prompting}.json", "w") as f:
                  f.write(template_d2m.replace("<<SEED>>", seed).replace("<<MODEL>>", model).replace("<<SWAP_COLUMNS>>", "true"))
              with open(f"./configs/doc2model/{project}_m2d_{seed}_{model}_{prompting}.json", "w") as f:
                  f.write(template_m2d.replace("<<SEED>>", seed).replace("<<MODEL>>", model).replace("<<SWAP_COLUMNS>>", "false"))

              # With Interfaces & Usages
              with open(f"./configs/doc2model/{project}_d2m_{seed}_{model}_{prompting}_iface_usages.json", "w") as f:
                  f.write(template_d2m.replace("<<SEED>>", seed).replace("<<MODEL>>", model).replace("<<SWAP_COLUMNS>>", "true").replace('"includeInterfaceRealizations" : false', '"includeInterfaceRealizations" : true').replace('"includeUsages" : false', '"includeUsages" : true'))
              with open(f"./configs/doc2model/{project}_m2d_{seed}_{model}_{prompting}_iface_usages.json", "w") as f:
                  f.write(template_m2d.replace("<<SEED>>", seed).replace("<<MODEL>>", model).replace("<<SWAP_COLUMNS>>", "false").replace('"includeInterfaceRealizations" : false', '"includeInterfaceRealizations" : true').replace('"includeUsages" : false', '"includeUsages" : true'))



              # With Interfaces & Usages & Operations
              with open(f"./configs/doc2model/{project}_d2m_{seed}_{model}_{prompting}_iface_usages_ops.json", "w") as f:
                  f.write(template_d2m.replace("<<SEED>>", seed).replace("<<MODEL>>", model).replace("<<SWAP_COLUMNS>>", "true").replace('"includeInterfaceRealizations" : false', '"includeInterfaceRealizations" : true').replace('"includeUsages" : false', '"includeUsages" : true').replace('"includeOperations" : false', '"includeOperations" : true'))
              with open(f"./configs/doc2model/{project}_m2d_{seed}_{model}_{prompting}_iface_usages_ops.json", "w") as f:
                  f.write(template_m2d.replace("<<SEED>>", seed).replace("<<MODEL>>", model).replace("<<SWAP_COLUMNS>>", "false").replace('"includeInterfaceRealizations" : false', '"includeInterfaceRealizations" : true').replace('"includeUsages" : false', '"includeUsages" : true').replace('"includeOperations" : false', '"includeOperations" : true'))