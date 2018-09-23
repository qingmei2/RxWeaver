# RxWeaver

<a href='https://bintray.com/mq2553299/maven/rxweaver/_latestVersion'><img src='https://api.bintray.com/packages/mq2553299/maven/rxweaver/images/download.svg'></a>

## 简介

关于`RxJava`的全局的error处理，通用的方案是通过 **继承**，将处理的逻辑加入到放入`Observer`导出类的`onError()`中，这种方式相对 **简单** 且 **易于上手**，但限制性在于：

* 1.开发者无法在`subscribe()`中使用lambda，灵活性降低， 这也是为什么强调尽量考虑 **组合** 的设计方式而不直接采用 **继承** 的原因。
* 2.如果开发者想要移除（或者重构）相关代码，就必须改动既有的业务逻辑，比如每一个网络请求的`subscribe()`中的代码，这种逻辑改动是产品级的。
* 3.`onError()`中对error进行处理，这意味着打破了`Observable`的链式调用(这种代码真的很不Reactive和Functional！)，开发者不得不在回调中`subscribe()`一个新的`Observable`—— `RxJava`版的 **Callback Hell（回调地狱）** 产生了。

RxWeaver是轻量且灵活的RxJava2 **全局Error处理中间件** ，类似 **AOP** 的思想，在既有的代码上  **插入** 或 **删除**  一行代码，即可实现全局Error处理的需求——而不是破坏`RxJava`所表达的 **响应式编程** 和 **函数式编程** 的思想。


## 特性

* 1.**轻量**：整个工具库只有4个类共不到200行代码，jar包体积仅3kb;
* 2.**无额外依赖**：所有代码的实现都依赖于`RxJava`本身的原生操作符;
* 3.**高扩展性**：开发者可以通过接口实现任意复杂的需求实现，详情请参考下文;
* 4.**灵活**：不破坏既有的业务代码，而是在原本的流中 **插入** 或 **删除** 一行代码——它是 **可插拔的**。

### How to use it?

### 1.添加依赖

```groovy
implementation 'com.github.qingmei2.rxweaver:rxweaver:0.2.1'        // Writen by Kotlin
implementation 'com.github.qingmei2.rxweaver:rxweaver_java:0.2.1'   // Writen by Java
```

RxWeaver默认的开发分支为`kotlin`,`Java`版本的 源码和示例代码请参考 **[这里](https://github.com/qingmei2/RxWeaver/tree/java)** 。

### 2.配置GlobalErrorTransformer

[GlobalErrorTransformer](https://github.com/qingmei2/RxWeaver/blob/kotlin/rxweaver/src/main/java/com/github/qingmei2/core/GlobalErrorTransformer.kt) 是一个是`Transformer<T, R>`的实现类，负责把全局的error处理逻辑，分发给不同的 **响应式类型**(Observable、Flowable、Single、Maybe、Completable)：

```Kotlin
class GlobalErrorTransformer<T> constructor(
        private val globalOnNextRetryInterceptor: (T) -> Observable<T> = { Observable.just(it) },
        private val globalOnErrorResume: (Throwable) -> Observable<T> = { Observable.error(it) },
        private val retryConfigProvider: (Throwable) -> RetryConfig = { RetryConfig() },
        private val globalDoOnErrorConsumer: (Throwable) -> Unit = { },
        private val upStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() },
        private val downStreamSchedulerProvider: () -> Scheduler = { AndroidSchedulers.mainThread() }
) : ObservableTransformer<T, T>, FlowableTransformer<T, T>, SingleTransformer<T, T>,  MaybeTransformer<T, T>, CompletableTransformer {
      // ...
}
```

配置一个函数，保证能够返回`GlobalErrorTransformer`的实例：

object RxUtils {

  fun <T> handleGlobalError(activity: FragmentActivity): GlobalErrorTransformer<T>{
      return .....
  }
}

[点击这里](https://github.com/qingmei2/RxWeaver/blob/kotlin/sample/src/main/java/com/github/qingmei2/RxUtils.kt)查看sample中的配置方式示例。

### 3.对需要进行全局error处理的RxJava流中添加这行代码：

```kotlin
private fun requestHttp(observable: Observable<UserInfo>) {
    observable
            .compose(RxUtils.handleGlobalError<UserInfo>(this))  // 将上面的接口配置给Observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                  // ....
            }
}
```

## 功能

下面通过几个案例展示RxWeaver的功能支持，你也可以运行sample并运行查看效果。

### 1.全局异常配置，弹出toast：

![](https://github.com/qingmei2/RxWeaver/blob/kotlin/screenshots/1.gif)

### 2.全局异常配置，弹出Dialog并是否重试：

![](https://github.com/qingmei2/RxWeaver/blob/kotlin/screenshots/2.gif)

### 3.全局异常配置，token异常，重新登录后返回界面重试：

![](https://github.com/qingmei2/RxWeaver/blob/kotlin/screenshots/3.gif)

### 4.更多私有化定制的需求....

## 原理

**RxWeaver** 是一个轻量且灵活的RxJava2 **全局Error处理中间件** ，这意味着，它并不是一个非常重，设计非常复杂的框架。**Weaver** 翻译过来叫做 **编织鸟**， 可以让开发者对error处理逻辑 **组织**，以达到实现全局Error的把控。

它的核心原理是依赖`compose()`操作符——这是RxJava给我们提供的可以面向 **响应式数据类型** (Observable/Flowable/Single等等)进行 **AOP** 的接口, 可以对响应式数据类型 **加工** 、**修饰** ，甚至 **替换**。

它的原理也是非常 **简单** 的，只要熟悉了`onErrorResumeNext`、`retryWhen`、`doOnError`这几个关键的操作符，你就可以马上上手对应的配置；它也是非常 **轻量** 的，轻到甚至可以直接把源代码复制粘贴到自己的项目中，通过jcenter依赖，它的体积也只有3kb。

## 关于RxWeaver

在不断深入学习理解`RxJava`的过程中，我沉浸`RxJava`不可自拔，1年多前，如果问我`RxJava`能够实现什么，我可能会侃侃而谈；但是如今，我反而回答不了你，或者会尝试反问，你觉得`RxJava`实现不了什么？

它太强大了！正如[这篇文章](https://juejin.im/post/5b8f5f0ee51d450ea52f6a37)所描述的：

> RxJava 的操作符是我们平时处理业务逻辑时常用方法的 **高度抽象**.

**高度抽象** 意味着学习曲线的陡峭性，我已经被`RxJava`所征服，因此我希望能把我自己的一些理解分享给大家——它不一定是最优秀的方案，但是如果它能让你在使用过程中增加对`RxJava`的理解，这就是值得的。

我非常喜欢`RxWeaver`,有朋友说说它代码有点少，但我却认为 **轻量** 是它最大的优点，`RxWeaver`的本质目的是帮助开发者 **对业务逻辑进行组织**，使其能够写出更 **Reactive** 和 **Functional** 的代码*。

## License

    The RxWeaver: MIT License

    Copyright (c) 2018 qingmei2

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
