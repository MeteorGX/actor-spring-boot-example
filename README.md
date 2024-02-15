# H5游戏服务端

基于 [actor-spring-boot-starter](https://github.com/MeteorGX/actor-spring-boot-starter) 设计的 WebSocket 服务端

游戏客户端则是基于 [actor-spring-boot-client](https://github.com/MeteorGX/actor-spring-boot-client) 开发,
这里仅仅做H5网络游戏开发设计的具体流程.

## 协议对接

可以查看 UML 流程, 最基本的授权流程如下:

![UML](game.drawio.png)

注意通讯协议并不是服务端独有, 通讯协议是 `服务端和客户端` 共同维护处理, 这里已经采用 `python` 编写好协议同步工具:

```shell
# 将内部协议 JSON 转化成支持 Java/Python/Lua/C# 等格式文件
# 客户端和服务端可以直接转化并取用
.\tools\protocol.py -i .\protocol\ -o .\target\
```

> 协议最好独立版本库让客户端和服务端一起维护, 可以直接建立 GIT|SVN 库来另外同步

