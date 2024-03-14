# !/usr/bin/python
# -*- coding: UTF-8 -*-

# ===============================================
# Example:
#   csv2json.py -i <input_dir> -o <output_dir> -e <encode:(default utf-8)> -d <delimiter:(default ,)> -f <format:(default @)>
# CSV File:
#   策划可视文件名@代码声明类名.csv -->  物品信息@Item.csv
# ===============================================


import pathlib
import sys
import getopt
import json
import csv

# 帮助信息
def print_help(status = 0):
    print("Example:\r\n\tcsv2json.py -i <input_dir> -o <output_dir> -e <encode:(default utf-8)> -d <delimiter:(default ,)> -f <format:(default @)>")
    sys.exit(status)

# 查询目录符合条件的csv文件
def search_csv_file(path,format):
    return [f for f in pathlib.Path(path).iterdir() if f.is_file() and f.glob(".csv") and f.glob(format)]


# 入口方法
def main(argv):
    input_dir = ''
    output_dir = ''
    encoding = 'utf-8'
    delimiter = ','
    format = "@"

    try:
        opts, args = getopt.getopt(argv, "hi:o:e:d:f:", ["input_dir=", "output_dir=", "encode=", "delimiter=","format="])
    except getopt.GetoptError:
        print_help()

    for opt, arg in opts:
        if opt == '-h':
            print_help()
        elif opt in ("-i", "--input_dir"):
            input_dir = arg
        elif opt in ("-o", "--output_dir"):
            output_dir = arg
        elif opt in ("-e", "--encode"):
            encoding = arg
        elif opt in ("-d", "--delimiter"):
            delimiter = arg
        elif opt in ("-f", "--format"):
            format = arg

    if len(input_dir) <= 0 or len(output_dir) <= 0:
        print("错误: 找不到输入|输出目录信息")
        print_help()

    print("输入目录: ", input_dir)
    print("输出目录: ", output_dir)


    files = search_csv_file(input_dir,format)
    if len(files) <= 0:
        print("找不到策划文件")
        print_help()

    # 遍历文件
    csv_list = []
    for file in files:
        pos = file.name.find(format)
        if pos != -1:
            name = file.name[pos + 1:]
            name = name.replace(".csv", "")
            if len(name) > 0:
                name = name + ".json"
                print("加载策划文件: ",file, "----->", name)
                csv_list.append({
                    "file": file,
                    "name": name,
                })

    # 遍历数据
    for table in csv_list:
        table_name = table["file"]
        table_file = table["name"]
        with open(table_name, mode="r", encoding=encoding) as file:
            reader = csv.reader(file, delimiter=delimiter)

            # 忽略首行类型数据
            _ignore = next(reader)
            keys = next(reader)
            types = next(reader)
            if len(keys) != len(types) and len(types) <= 0:
                break

            # 打印目前的csv标识和类型
            count = len(keys)
            print("属性名称: ",keys)
            print("属性类型: ",types)

            # 这里必须要认准 KEY 标识设计成 KEY-VALUE 方式
            lines = {}
            for row in reader:
                if len(row) < count:
                    break

                line = {}
                keyboard = ""
                for i in range(count):
                    row_type = types[i].lower()
                    row_key = keys[i].lower()
                    row_value = row[i]

                    if row_type == "int":
                        try:
                            row_value = int(row_value)
                        except ValueError:
                            row_value = None
                    elif row_type == "float":
                        try:
                            row_value = float(row_value)
                        except ValueError:
                            row_value = None
                    elif row_type == "string":
                        row_value = str(row_value)
                    elif row_type == "bool":
                        row_value = bool(row_value)
                    elif row_type == "array":
                        row_value = json.loads(row_value)
                        if not isinstance(row_value,list):
                            row_value = None
                    elif row_type == "object":
                        row_value = json.loads(row_value)
                        if not isinstance(row_value,dict):
                            row_value = None
                    else:
                        row_value = None

                    if row_key == "key":
                        keyboard = str(row_value)
                    line[row_key] = row_value

                if len(keyboard) > 0:
                    lines[keyboard] = line

            # 获取到数据写入JSON
            if len(lines) >0:
                f = pathlib.Path(output_dir)
                f = f.joinpath(table_file)
                print("数据文件: ",f)
                print("数据合集: ",lines)
                with f.open(mode="w+", encoding="utf-8") as fd:
                    json.dump(lines, fd, ensure_ascii=False)


# 入口调用
if __name__ == "__main__":
    main(sys.argv[1:])