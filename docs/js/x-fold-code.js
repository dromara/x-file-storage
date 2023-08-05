/**
 * 用于折叠代码块的插件
 */
window.$docsify.plugins.push(function (hook, vm) {
    //对象引用，在适当时机需要释放
    let objects = []

    /**
     * 初始化完成后调用，只调用一次，没有参数。
     */
    hook.init(function () {
        let div = document.createElement("div")
        div.innerHTML = `
        <style>
        .x-fold-code{
            position: relative;
            padding-bottom: 20px;
            background-color:#F8F8F8;
        }
        
        /* =============== 展开之后的样式部分 =============== */
        
        .x-fold-code .x-fold-code-mask,
        .x-fold-code .x-fold-code-action {
            display: none;
        }
        
        .x-fold-code-gd{
            position: absolute;
            left: 0;
            right: 0;
            bottom: 0;
            padding-top: 10px;
            padding-bottom: 4px;
            text-align: center;
            background-color:#F8F8F8;
            user-select: none;
        }
        
        .x-fold-code-gd.top{
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: auto;
        }
        
        .x-fold-code-gd.bottom{
            position: absolute;
            top: auto;
            left: 0;
            right: 0;
            bottom: 0;
        }
        
        .x-fold-code-gd.fixed{
            position: fixed;
            top: auto;
            left: 0;
            right: auto;
            bottom: 0;
            background: linear-gradient(-180deg, rgb(0 0 0 / 0%) 0%, rgb(248 248 248 / 70%) 40%, rgb(248 248 248 / 90%) 100%);
        }
        
        .x-fold-code-gd a {
            cursor: pointer;
        }
        
        /* =============== 折叠之后的样式部分 =============== */
        
        .x-fold-code.hidden{
            padding-bottom: 0;
        }
        .x-fold-code.hidden pre code {
            max-height: 300px;
            overflow: hidden;
        }
        
        .x-fold-code.hidden .x-fold-code-mask {
            background: linear-gradient(-180deg, rgb(0 0 0 / 0%) 0%, #cdcdcd 100%);
            position: absolute;
            left: 0;
            right: 0;
            bottom: 0;
            height: 80px;
            display: block;
        }
        .x-fold-code.hidden .x-fold-code-action {
            display: block;
            position: absolute;
            left: 0;
            right: 0;
            bottom: 10px;
            text-align: center;
            user-select: none;
        }
        .x-fold-code.hidden .x-fold-code-action a {
            cursor: pointer;
        }
        
        
        
        </style>`
        document.head.appendChild(div.querySelector("style"))
    });

    /**
     * 每次开始解析 Markdown 内容时调用
     */
    hook.beforeEach(function (content) {
        // console.log("hook.beforeEach", content)
        content = content.replaceAll(/<!--\s*fold:start\s+.*-->/g, (item, index) => {
            // let args = {}
            // item.substring(item.indexOf("fold:start ") + 11, item.lastIndexOf("-->"))
            //     .split(" ").map(v => v.trim()).filter(v => v)
            //     .map(v => v.split(":")).forEach(v => args[v[0]] = v[1])
            //
            // console.log("item", item, args)
            return `<div class='x-fold-code hidden'>`
        })

        content = content.replaceAll(/<!--\s*fold:end\s*-->/g, "</div > ")

        return content;
    });

    /**
     * 解析成 html 后调用。
     * beforeEach 和 afterEach 支持处理异步逻辑
     * 异步处理完成后调用 next(html) 返回结果
     */
    hook.afterEach(function (html, next) {
        // console.log("hook.afterEach", html, next)
        next(html);
    });

    /**
     * 每次路由切换时数据全部加载完成后调用，没有参数。
     */
    hook.doneEach(function () {
        for (let obj of objects) {
            if (obj instanceof ResizeObserver) {
                obj.disconnect()
            } else if (obj instanceof Function) {
                window.removeEventListener("scroll", obj)
            }
        }
        objects = []


        let preList = document.querySelectorAll(".x-fold-code pre[data-lang] code")

        for (let pre of preList) {
            let mask = document.createElement("div")
            mask.className = "x-fold-code-mask"
            pre.parentElement.parentElement.appendChild(mask)

            let action = document.createElement("div")
            action.className = "x-fold-code-action"
            pre.parentElement.parentElement.appendChild(action)

            let a = document.createElement("a")
            a.innerText = "👀点击展开全部代码"
            a.onclick = showAllCode
            action.appendChild(a)
        }

        console.log("hook.doneEach", preList)


    });

    /**
     * 展开所有代码
     * @param e {MouseEvent}
     */
    function showAllCode(e) {
        let div = e.target.parentElement.parentElement
        div.classList.remove('hidden')

        let gd = document.createElement("div")
        gd.className = "x-fold-code-gd"
        div.appendChild(gd)

        let a = document.createElement("a")
        a.innerText = "😝点击收起全部代码"
        a.onclick = hideAllCode
        gd.appendChild(a)

        let observer = new ResizeObserver(onScroll)
        observer.observe(div)

        window.addEventListener("scroll", onScroll)

        objects.push(observer, onScroll)

        function onScroll() {
            let rect = div.getBoundingClientRect();
            // console.log(rect)

            //滚动到顶部
            if (rect.y + gd.clientHeight > window.innerHeight) {
                if (!gd.classList.contains("top")) {
                    gd.classList.remove("bottom")
                    gd.classList.remove("fixed")
                    gd.classList.add("top")
                    gd.style.left = ""
                    gd.style.width = ""
                }
                return
            }

            let bottom = rect.bottom - window.innerHeight

            //滚动到底部
            if (bottom < 0) {
                if (!gd.classList.contains("bottom")) {
                    gd.classList.remove("top")
                    gd.classList.remove("fixed")
                    gd.classList.add("bottom")
                    gd.style.left = ""
                    gd.style.width = ""
                }
                return
            }

            //滚动中
            if (!gd.classList.contains("fixed")) {
                gd.classList.remove("top")
                gd.classList.remove("bottom")
                gd.classList.add("fixed")
            }
            gd.style.left = rect.left + "px"
            gd.style.width = rect.width + "px"
        }

        /**
         * 隐藏所有代码
         * @param e {MouseEvent}
         */
        function hideAllCode(e) {
            observer.disconnect()
            window.removeEventListener("scroll", onScroll)
            console.log(objects)
            objects = objects.filter(v => v !== observer && v !== onScroll)

            div.classList.add('hidden')
            div.removeChild(gd)
        }

    }


})
