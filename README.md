# RxWeaver

<a href='https://bintray.com/mq2553299/maven/rxweaver/_latestVersion'><img src='https://api.bintray.com/packages/mq2553299/maven/rxweaver/images/download.svg'></a>

关于这个repo的起源，请参考这篇文章：

[不要打破链式调用！一个极低成本的RxJava全局Error处理方案](https://juejin.im/post/5be7bb9f6fb9a049f069c706)

## 简介

关于`RxJava`的全局的error处理，通用的方案是通过 **继承**，将处理的逻辑加入到放入`Observer`导出类的`onError()`中，这种方式相对 **简单** 且 **易于上手**，但限制性在于：

* 1.开发者无法在`subscribe()`中使用lambda，灵活性降低， 这也是为什么强调尽量考虑 **组合** 的设计方式而不直接采用 **继承** 的原因。
* 2.如果开发者想要移除（或者重构）相关代码，就必须改动既有的业务逻辑，比如每一个网络请求的`subscribe()`中的代码，这种逻辑改动是产品级的。
* 3.`onError()`中对error进行处理，这意味着打破了`Observable`的链式调用。

RxWeaver是轻量且灵活的RxJava2 **全局Error处理中间件** ，类似 **AOP** 的思想，在既有的代码上  **插入** 或 **删除**  一行代码，即可实现全局Error处理的需求——而不是破坏`RxJava`所表达的 **响应式编程** 和 **函数式编程** 的思想。

> 对于这个repo更应像是一个思路的展示，如果你有兴趣，欢迎star或者fork ——我更建议您通过复制源码的方式在自己的项目中实现，如果有疑问，欢迎加QQ群391638630一起探讨 :smile。

## 特性

* 1.**轻量**：整个工具库只有4个类共不到200行代码，jar包体积仅3kb;
* 2.**无额外依赖**：所有代码的实现都依赖于`RxJava`本身的原生操作符;
* 3.**高扩展性**：开发者可以通过接口实现任意复杂的需求实现，详情请参考下文;
* 4.**灵活**：不破坏既有的业务代码，而是在原本的流中 **插入** 或 **删除** 一行代码——它是 **可插拔的**。

## Usage

### 1.添加依赖

```groovy
implementation 'com.github.qingmei2.rxweaver:rxweaver:0.3.0'
```

默认的开发分支为`kotlin`,`Java`版本的源码和示例代码请参考 **[这里](https://github.com/qingmei2/RxWeaver/tree/java)** 。

> 因个人精力有限，`Java`版本无法保证与`Kotlin`保持最新版本的同步，但是我尽量保证新的调整会尽快反映到`Java`分支上。

### 2.配置GlobalErrorTransformer

[GlobalErrorTransformer](https://github.com/qingmei2/RxWeaver/blob/kotlin/rxweaver/src/main/java/com/github/qingmei2/core/GlobalErrorTransformer.kt) 是一个是`Transformer<T, R>`的实现类，负责把全局的error处理逻辑，分发给不同的 **响应式类型**(Observable、Flowable、Single、Maybe、Completable)：

```Kotlin
class GlobalErrorTransformer<T> constructor(
        private val globalOnNextInterceptor: (T) -> Observable<T> = { Observable.just(it) },
        private val globalOnErrorResume: (Throwable) -> Observable<T> = { Observable.error(it) },
        private val retryConfigProvider: (Throwable) -> RetryConfig = { RetryConfig() },
        private val globalDoOnErrorConsumer: (Throwable) -> Unit = { }
) : ObservableTransformer<T, T>, FlowableTransformer<T, T>, SingleTransformer<T, T>,  MaybeTransformer<T, T>, CompletableTransformer {
      // ...
}
```

配置一个函数，保证能够返回`GlobalErrorTransformer`的实例：

```kotlin
fun <T> handleGlobalError(activity: FragmentActivity): GlobalErrorTransformer<T>{
  return .....
}
```

[点击这里](https://github.com/qingmei2/RxWeaver/blob/kotlin/sample/src/main/java/com/github/qingmei2/RxUtils.kt)查看sample中的配置方式示例。

### 3.对需要进行全局error处理的RxJava流中添加这行代码：

```kotlin
private fun requestHttp(observable: Observable<UserInfo>) {
    observable
            .compose(handleGlobalError<UserInfo>(this))  // 将上面的接口配置给Observable
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

<div align:left;display:inline;> <img width="200" height="360" src="https://github.com/qingmei2/RxWeaver/blob/kotlin/screenshots/1.gif"/> </div>


### 2.全局异常配置，弹出Dialog并是否重试：

<div align:left;display:inline;> <img width="200" height="360" src="https://github.com/qingmei2/RxWeaver/blob/kotlin/screenshots/2.gif"/> </div>


### 3.全局异常配置，token异常，重新登录后返回界面重试：

<div align:left;display:inline;> <img width="200" height="360" src="https://github.com/qingmei2/RxWeaver/blob/kotlin/screenshots/3.gif"/> </div>

### 4.更多

当然也能够实现更多私有化定制的需求....

## 原理&如何配置GlobalErrorTransformer

**RxWeaver** 是一个轻量且灵活的RxJava2 **全局Error处理组件** ，这意味着，它并不是一个设计非常复杂的框架。**Weaver** 翻译过来叫做 **编织鸟**， 可以让开发者对error处理逻辑 **组织**，以达到实现全局Error的把控。

它的核心原理是依赖 `compose()` 操作符——这是RxJava给我们提供的可以面向 **响应式数据类型** (Observable/Flowable/Single等等)进行 **AOP** 的接口, 可以对响应式数据类型 **加工** 、**修饰** ，甚至 **替换**。

它的原理也是非常 **简单** 的，只要熟悉了 `onErrorResumeNext` 、 `retryWhen` 、 `doOnError` 这几个关键的操作符，你就可以马上上手对应的配置；它也是非常 **轻量** 的，轻到甚至可以直接把源代码复制粘贴到自己的项目中，通过jcenter依赖，它的体积也只有3kb。

### 1. globalOnNextInterceptor: (T) -> Observable<T>

**将正常数据转换为一个异常** 是很常见的需求，有时流中的数据可能会是一个错误的状态（比如，token失效）。

`globalOnNextInterceptor` 函数内部直接使用 `flatMap()` 操作符将数据进行了转换，因此你可以将一个错误的状态转换为 `Observable.error()` 向下传递：

```kotlin
globalOnNextInterceptor = {
    when (it.statusCode) {
        STATUS_UNAUTHORIZED -> {        // token 失效，将流转换为error，交由下游处理
            Observable.error(TokenExpiredException())
        }
        else -> Observable.just(it)     // 其他情况，数据向下游正常传递
    }
}
```

### 2. globalOnErrorResume: (Throwable) -> Observable<T>

和 `globalOnNextInterceptor` 函数很相似，更常见的情况是通过解析不同的 `Throwable` ，然后根据实际业务做出对应的处理：

```kotlin
globalOnErrorResume = { error ->
    when (error) {
        is ConnectException -> {        // 连接错误，转换为特殊的异常（标记作用），交给下游
            Observable.error<T>(ConnectFailedAlertDialogException())
        }
        else -> Observable.error<T>(error)  // 其他情况，异常向下游正常传递
    }
}
```

`globalOnErrorResume` 函数内部是通过 `onErrorResumeNext()` 操作符实现的。

### 3.retryConfigProvider: (Throwable) -> RetryConfig

当需要做出是否要重试的决定时，需要根据异常的类型进行判断，并做出对应的行为：

```kotlin
retryConfigProvider = { error ->
    when (error) {
        is ConnectFailedAlertDialogException -> RetryConfig {
            // .....
        }
        is TokenExpiredException -> RetryConfig(delay = 3000) {
            // .....
        }
        else -> RetryConfig() // 其它异常都不重试
    }
}
```

`retryConfigProvider` 函数内部是通过 `retryWhen()` 操作符实现的。


### 4.globalDoOnErrorConsumer: (Throwable) -> Unit

`globalDoOnErrorConsumer` 函数并不会拦截或消费上游发射的异常，它的内部实际上就是通过 `doOnError()` 操作符的调用，做一些 **顺势而为** 的处理,比如toast，或者其它。

```kotlin
globalDoOnErrorConsumer = { error ->
    when (error) {
        is JSONException -> {
            Toast.makeText(activity, "全局异常捕获-Json解析异常！", Toast.LENGTH_SHORT).show()
        }
        else -> {

        }
    }
}
```

## 关于RxJava

在不断深入学习理解 `RxJava` 的过程中，我沉浸 `RxJava` 不可自拔，1年多前，如果问我 `RxJava` 能够实现什么，我可能会侃侃而谈；但是如今，我反而回答不了你，或者会尝试反问，你觉得 `RxJava` 实现不了什么？

它太强大了！正如[这篇文章](https://juejin.im/post/5b8f5f0ee51d450ea52f6a37)所描述的：

> RxJava 的操作符是我们平时处理业务逻辑时常用方法的 **高度抽象**.

**高度抽象** 意味着学习曲线的陡峭性，我希望能把我自己的一些理解分享给大家——它不一定是最优秀的方案，但是如果它能让你在使用过程中增加对 `RxJava` 的理解，这就是值得的。

我非常喜欢 `RxWeaver` 的设计,有朋友说说它代码有点少，但我却认为 **轻量** 是它最大的优点，它创建 **最初的目的** 就是帮助开发者 **对业务逻辑进行组织**，使其能够写出更 **Reactive** 和 **Functional** 的代码。

## 我的其他RxJava项目

* [RxImagePicker: Support for RxJava2. Flexible picture selector of Android, provides the support for theme of Zhihu and WeChat.](https://github.com/qingmei2/RxImagePicker)

* [MVVM-Rhine: The MVVM using RxJava and Android databinding.](https://github.com/qingmei2/MVVM-Rhine)

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
