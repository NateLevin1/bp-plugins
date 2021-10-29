import os
for path in os.listdir("./out/artifacts/"):
    if path == ".DS_Store":
        continue
    new_name = f"{path[0:len(path)-4]}.jar"
    os.system(f"cp ./out/artifacts/{path}/{new_name} ~/Downloads/server/plugins/{new_name}")
