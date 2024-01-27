#!/usr/bin/python
# -*- coding: UTF-8 -*-

import pathlib
import sys
import getopt
import json


# 识别是否为数字
def is_number(s):
    try:
        float(s)
        return True
    except ValueError:
        pass

    try:
        import unicodedata
        unicodedata.numeric(s)
        return True
    except (TypeError, ValueError):
        pass
    return False


# 查询目录所有JSON文件
def search_json_file(path):
    return [f for f in pathlib.Path(path).iterdir() if f.is_file() and f.glob(".json")]


# 构建Java
def out_java_proto(output, data, name="Protocols", file="Protocols.java"):
    f = pathlib.Path(output)
    f = f.joinpath(file)
    text = '''
/**
 * 请求响应数据协议
 */
'''
    text += "public class %s {\n" % name

    for v in data:
        text += "    public static final int %s = %d; // %s\n" % (v["name"], v["value"], v["desc"])

    text += "}"

    f.open(mode="w+", encoding="utf-8")
    f.write_text(data=text, encoding="utf-8")


# 构建Godot
def out_godot_proto(output, data, name="Protocols", file="Protocols.gd"):
    f = pathlib.Path(output)
    f = f.joinpath(file)
    text = '# 请求响应数据协议: %s\n' % name
    text += 'extends Node\n'
    for v in data:
        text += "const %s:int = %d; # %s\n" % (v["name"], v["value"], v["desc"])

    f.open(mode="w+", encoding="utf-8")
    f.write_text(data=text, encoding="utf-8")


# 构建Lua
def out_lua_proto(output, data, name="Protocols", file="Protocols.lua"):
    f = pathlib.Path(output)
    f = f.joinpath(file)
    text = '---\n'
    text += '--- 请求响应数据协议\n'
    text += '---\n'
    text += '%s = %s or  {\n' % (name, name)
    for v in data:
        text += "    %s = %d; -- %s\n" % (v["name"], v["value"], v["desc"])
    text += '}\n'
    f.open(mode="w+", encoding="utf-8")
    f.write_text(data=text, encoding="utf-8")


# 构建C#
def out_csharp_proto(output, data, name="Protocols", file="Protocols.cs"):
    f = pathlib.Path(output)
    f = f.joinpath(file)
    text = '''
/**
 * 请求响应数据协议
 */
'''
    text += "public class %s {\n" % name

    for v in data:
        text += "    public static readonly int %s = %d; // %s\n" % (v["name"], v["value"], v["desc"])

    text += "}"

    f.open(mode="w+", encoding="utf-8")
    f.write_text(data=text, encoding="utf-8")


# 入口方法
def main(argv):
    input_dir = ''
    output_dir = ''
    try:
        opts, args = getopt.getopt(argv, "hi:o:", ["input_dir=", "output_dir="])
    except getopt.GetoptError:
        print('protocol.py -i <input_dir> -o <output_dir>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print("protocol.py -i <input_dir> -o <output_dir>")
            sys.exit()
        elif opt in ("-i", "--input_dir"):
            input_dir = arg
        elif opt in ("-o", "--output_dir"):
            output_dir = arg

    if len(input_dir) <= 0 or len(output_dir) <= 0:
        print("找不到输入|输出目录信息")
        sys.exit(1)

    print("输入目录: ", input_dir)
    print("输出目录: ", output_dir)

    files = search_json_file(input_dir)
    if len(files) <= 0:
        print("找不到协议文件")
        sys.exit()

    # 遍历数据
    proto_list = []
    for file in files:
        prop = file.name.upper().replace(".JSON", "")
        print("协议分类: ", prop)

        text = file.read_text(encoding="utf-8")
        data = json.loads(text)
        if isinstance(data, dict):
            for value in data:
                name = data[value]["name"]
                desc = data[value]["description"]
                proto = "%s_%s" % (prop, name)
                print("解析协议: ", value, "-", proto, "|", desc)
                proto_list.append({
                    "value": int(value),
                    "name": proto,
                    "desc": desc
                })

    # 构建协议
    out_godot_proto(output_dir, proto_list)
    out_lua_proto(output_dir, proto_list)
    out_csharp_proto(output_dir, proto_list)
    out_java_proto(output_dir, proto_list)


# 入口调用
if __name__ == "__main__":
    main(sys.argv[1:])
