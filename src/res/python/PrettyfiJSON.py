import sys, os, json, pprint

current_dir = os.getcwd()
input_file_name = input("File name: ")
input_file = open(input_file_name).read()

output_file = open(current_dir+"/prettified_"+input_file_name, "w+")

json_data = json.loads(input_file)

for data in json_data:


pprint.pprint(json_data, stream=output_file, indent=0)
