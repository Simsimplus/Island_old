![ic_launch](ic_island_boat.png)

这是完全由kotlin编写的[A岛匿名版](https://adnmb3.com/Forum)安卓客户端，app的架构遵守了开发指南所推荐的[MVVM架构](https://developer.android.com/jetpack/guide#recommended-app-arch),并且使用了Hilt,  Coroutines, Flow, Jetpack (Room, ViewModel, LiveData) 等多种组件。
## 功能
1. 发串与回复
2. 自定义屏蔽项
3. 自定义布局（现仅限于FAB设置）
4. 导入饼干（二维码，用户系统）
5. 收藏串
## 待实现
1. 自定义主题颜色
2. 串内图片滑动浏览
3. 根据反馈加入实用功能
4. 优化性能

| 功能 | 依赖          |
|------|---------------|
| 列表 | recycler view |
|数据库|room|
