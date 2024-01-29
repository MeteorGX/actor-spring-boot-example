# !/usr/bin/python
# -*- coding: UTF-8 -*-

import pathlib
import sys
import getopt
import json
import csv


# 查询目录所有csv文件
def search_csv_file(path):
    return [f for f in pathlib.Path(path).iterdir() if f.is_file() and f.glob(".csv")]


# 入口方法
def main(argv):
    input_dir = ''
    output_dir = ''
    encoding = 'utf-8'
    delimiter = ','

    try:
        opts, args = getopt.getopt(argv, "hi:o:e:d:", ["input_dir=", "output_dir=", "encode=", "delimiter="])
    except getopt.GetoptError:
        print('csv2json.py -i <input_dir> -o <output_dir> -e <encode:(default utf-8)> -d <delimiter:(default ,)>')
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print("csv2json.py -i <input_dir> -o <output_dir> -e <encode:(default utf-8)> -d <delimiter:(default ,)>")
            sys.exit()
        elif opt in ("-i", "--input_dir"):
            input_dir = arg
        elif opt in ("-o", "--output_dir"):
            output_dir = arg
        elif opt in ("-e", "--encode"):
            encoding = arg
        elif opt in ("-d", "--delimiter"):
            delimiter = arg

    if len(input_dir) <= 0 or len(output_dir) <= 0:
        print("找不到输入|输出目录信息")
        sys.exit(1)

    print("输入目录: ", input_dir)
    print("输出目录: ", output_dir)

    files = search_csv_file(input_dir)
    if len(files) <= 0:
        print("找不到策划文件")
        sys.exit()

    # 遍历数据
    csv_list = []
    for file in files:
        pos = file.name.find("#")
        if pos != -1:
            name = file.name[pos + 1:]
            name = name.replace(".csv", "")
            if len(name) > 0:
                name = name + ".json"
                print("加载策划文件: ", name)
                csv_list.append({
                    "file": file,
                    "name": name,
                })

    # 开始写入策划文件
    for table in csv_list:
        with open(table["file"], mode="r", encoding=encoding) as file:
            reader = csv.reader(file, delimiter=delimiter)

            # 获取首行类型数据
            headers = next(reader)
            if len(headers) <= 0:
                break

            # 遍历出类型数据
            names = []
            for header in headers:
                names.append(header)

            if len(names) <= 0:
                print("表格错误", table["name"])
                break

            # 检索出注释, 用于给策划看的内容, 可以跳过
            _descriptions = next(reader)

            # 检索出数据
            data = []
            for row in reader:
                # 没有数据就跳过
                if len(row) <= 0:
                    break

                # 筛选数据
                lines = {}
                for i, name in enumerate(names):
                    try:
                        value = row[i]
                        line = json.loads(value)
                    except IndexError:
                        value = None
                    except json.decoder.JSONDecodeError:
                        if isinstance(value, str):
                            line = str(value)
                        else:
                            line = None
                    lines[name] = line
                data.append(lines)

            # 写入文件
            if len(data) > 0:
                f = pathlib.Path(output_dir)
                f = f.joinpath(table["name"])
                fd = f.open(mode="w+", encoding="utf-8")
                json.dump(data, fd, ensure_ascii=False)


# 入口调用
if __name__ == "__main__":
    main(sys.argv[1:])
