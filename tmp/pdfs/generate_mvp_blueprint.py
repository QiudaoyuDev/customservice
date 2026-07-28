from reportlab.lib import colors
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Flowable

OUT = r"C:\Users\HP\Documents\customservice\output\pdf\海外硬件AI智能客服MVP功能拆解与实施方案.pdf"
pdfmetrics.registerFont(TTFont("CN", r"C:\Windows\Fonts\simhei.ttf"))

NAVY=HexColor("#0B1F33"); BLUE=HexColor("#176B87"); TEAL=HexColor("#2A9D8F"); ORANGE=HexColor("#F4A261")
INK=HexColor("#233342"); MUTED=HexColor("#61727E"); LINE=HexColor("#D6E1E6")
LB=HexColor("#EAF4F7"); LT=HexColor("#E9F6F2"); LO=HexColor("#FFF2DE"); LP=HexColor("#EFF0FA"); CREAM=HexColor("#F8F5EF")
S=getSampleStyleSheet()
S.add(ParagraphStyle(name="MTitle",fontName="CN",fontSize=27,leading=37,textColor=colors.white,spaceAfter=7))
S.add(ParagraphStyle(name="MSub",fontName="CN",fontSize=12,leading=19,textColor=HexColor("#D8E8EF")))
S.add(ParagraphStyle(name="MH1",fontName="CN",fontSize=19,leading=27,textColor=NAVY,spaceBefore=1,spaceAfter=8))
S.add(ParagraphStyle(name="MH2",fontName="CN",fontSize=12.5,leading=18,textColor=BLUE,spaceBefore=5,spaceAfter=4))
S.add(ParagraphStyle(name="MBody",fontName="CN",fontSize=8.6,leading=14,textColor=INK,spaceAfter=5))
S.add(ParagraphStyle(name="MSmall",fontName="CN",fontSize=7.2,leading=10.5,textColor=MUTED))
S.add(ParagraphStyle(name="MCardTitle",fontName="CN",fontSize=10.3,leading=14,textColor=NAVY,spaceAfter=3))
S.add(ParagraphStyle(name="MCardBody",fontName="CN",fontSize=7.7,leading=12,textColor=INK))
S.add(ParagraphStyle(name="MHeader",fontName="CN",fontSize=8.5,leading=12,textColor=colors.white))
S.add(ParagraphStyle(name="MQuote",fontName="CN",fontSize=12.5,leading=20,textColor=NAVY,alignment=TA_CENTER))

ALIASES={"title":"MTitle","sub":"MSub","h1":"MH1","h2":"MH2","body":"MBody","small":"MSmall","cardtitle":"MCardTitle","cardbody":"MCardBody","quote":"MQuote"}
def P(x, st="MBody"): return Paragraph(x, S[ALIASES.get(st,st)])

def footer(c,doc):
    if doc.page==1: return
    c.saveState(); c.setStrokeColor(LINE); c.line(18*mm,13*mm,192*mm,13*mm)
    c.setFillColor(MUTED); c.setFont("CN",7.2); c.drawString(18*mm,8*mm,"海外硬件 AI 智能客服平台 - MVP 功能拆解与实施方案")
    c.drawRightString(192*mm,8*mm,str(doc.page)); c.restoreState()

def table(rows, widths, header=True, font=7.6):
    cooked=[]
    for ri,row in enumerate(rows):
        cooked.append([cell if isinstance(cell,Paragraph) else P(cell,"MHeader" if header and ri==0 else "MCardBody") for cell in row])
    t=Table(cooked,colWidths=widths,repeatRows=1 if header else 0)
    style=[("GRID",(0,0),(-1,-1),.45,LINE),("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),6),("RIGHTPADDING",(0,0),(-1,-1),6),("TOPPADDING",(0,0),(-1,-1),6),("BOTTOMPADDING",(0,0),(-1,-1),6)]
    if header: style += [("BACKGROUND",(0,0),(-1,0),NAVY),("TEXTCOLOR",(0,0),(-1,0),colors.white)]
    t.setStyle(TableStyle(style)); return t

def card(title,text,bg=colors.white,w=174*mm):
    t=Table([[P(title,"MCardTitle")],[P(text,"MCardBody")]],colWidths=[w])
    t.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),bg),("BOX",(0,0),(-1,-1),.55,LINE),("LEFTPADDING",(0,0),(-1,-1),8),("RIGHTPADDING",(0,0),(-1,-1),8),("TOPPADDING",(0,0),(-1,-1),7),("BOTTOMPADDING",(0,0),(-1,-1),7)])); return t

class Flow(Flowable):
    def __init__(self,labels,width=493,height=80): Flowable.__init__(self); self.labels=labels; self.width=width; self.height=height
    def draw(self):
        c=self.canv; n=len(self.labels); gap=9; bw=(self.width-gap*(n-1))/n; bh=48; y=16; cols=[LB,LT,LO,LP,LB,LT]
        for i,label in enumerate(self.labels):
            x=i*(bw+gap); c.setFillColor(cols[i%len(cols)]); c.setStrokeColor(LINE); c.roundRect(x,y,bw,bh,7,fill=1,stroke=1)
            c.setFillColor(NAVY); c.setFont("CN",7.5); lines=label.split("\n"); sy=y+bh/2+(len(lines)-1)*5
            for j,line in enumerate(lines): c.drawCentredString(x+bw/2,sy-j*11,line)
            if i<n-1:
                c.setStrokeColor(TEAL); c.setLineWidth(1.3); c.line(x+bw+1,y+bh/2,x+bw+gap-2,y+bh/2)
                c.setFillColor(TEAL); c.circle(x+bw+gap-2,y+bh/2,1.7,fill=1,stroke=0)

doc=BaseDocTemplate(OUT,pagesize=A4,leftMargin=18*mm,rightMargin=18*mm,topMargin=17*mm,bottomMargin=19*mm)
fr=Frame(doc.leftMargin,doc.bottomMargin,doc.width,doc.height,id="main")
doc.addPageTemplates([PageTemplate(id="main",frames=[fr],onPage=footer)])
story=[]

# 1 cover
cover=Table([[P("海外硬件 AI 智能客服平台\nMVP 功能拆解与实施方案","title")],[P("覆盖功能边界、数据模型、实施阶段、验收标准与关键难点处理。","sub")]],colWidths=[174*mm])
cover.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),NAVY),("LEFTPADDING",(0,0),(-1,-1),14*mm),("RIGHTPADDING",(0,0),(-1,-1),14*mm),("TOPPADDING",(0,0),(-1,0),18*mm),("BOTTOMPADDING",(0,1),(-1,-1),16*mm)]))
story += [Spacer(1,20*mm),cover,Spacer(1,14*mm),P("面向合伙人、产品、研发与首个试点客户的共用蓝图","h2"),P("讨论版 v1.0 | 2026.07","body"),Spacer(1,12*mm),card("首个 MVP 的成功定义","用户扫码后，平台能准确确定设备范围；针对高频问题以安全、可验证的步骤引导用户；无法解决时把完整上下文交给人工；人工解决经验经审核后沉淀为新知识。",CREAM),Spacer(1,14*mm),P("本报告中的工期为资源假设，不构成任何交付承诺。实际范围必须以试点数据审计和客户投入为准。","small"),PageBreak()]

# 2 understanding boundary
story += [P("01 目标、边界与决策依据","h1"),P("产品要解决的不是“有没有聊天入口”，而是如何把正确的产品知识，在正确的版本和地区，以安全步骤交付给终端用户。","body")]
story += [table([["类别","内容","对 MVP 的影响"],["用户已明确的事实","出海硬件厂商；扫码进入；图文对话；知识管理；人工接入；模型配置；私有化部署。","必须有产品上下文、知识治理、人工协同和独立实例边界。"],["设计假设","可获得说明书、历史工单和售后专家；高频问题可标准化。","阶段 0 必须验证；否则不应进入全面开发。"],["待验证事项","首个品类、语言、人工渠道、数据质量、预算、私有化的真实约束。","决定范围与架构取舍，不能由团队臆定。"]],[37*mm,69*mm,68*mm]),Spacer(1,7*mm),P("首个 MVP 范围","h2"),table([["纳入","暂不纳入"],["1 家客户、1 个品类、1 个产品系列；扫码 Web/PWA；英语 + 1 门目标语言；文字/图片/错误码；文档知识 + 高频诊断流程；一种人工接入；客户独立实例 + 外部模型 API。","全品类、电话机器人、自动退款/换货、复杂维修/备件、完全离线模型、未经审核的自动知识发布。"]],[87*mm,87*mm]),Spacer(1,8*mm),P("阶段闸门","h2"),P("如果无法确认产品上下文、没有知识审核责任人、或无法取得一个真实人工接入渠道，MVP 应停留在阶段 0，优先补齐业务条件。","quote"),PageBreak()]

# 3 roles journey
story += [P("02 用户角色与核心旅程","h1"),table([["角色","目标","关键动作"],["终端用户","快速恢复使用或得到明确售后路径","扫码、描述问题、上传图片、执行步骤、确认结果。"],["人工客服","不重复问问题，处理复杂例外","查看交接包、补充处理、回写最终结果。"],["售后专家","把经验变成可复用资产","审核知识、编写诊断流程、定义安全边界。"],["客户管理员","控制产品、知识、模型、渠道","配置产品/二维码/权限/渠道，查看运营数据。"],["平台运维","保证交付与可靠性","开通实例、监控调用、定位故障、升级版本。"]],[28*mm,47*mm,99*mm]),Spacer(1,7*mm),P("用户旅程","h2"),Flow(["扫码\n确定产品", "描述问题\n图片/错误码", "意图路由", "检索或执行\n诊断流程", "确认解决\n或转人工"]),Spacer(1,7*mm),card("关键体验原则","先问最有区分度的问题；一次只给一个可操作步骤；说明预期现象；没有依据不编造；高风险先停止；转人工时用户无需从头描述。",LB),Spacer(1,6*mm),P("二维码与设备上下文","h2"),P("二维码建议承载 tenantId、productModelId、region、可选 batch/serial 及签名令牌。服务端必须验签，前端参数不能直接决定知识范围。无效二维码进入手动选品降级页，并保留异常审计。","body"),PageBreak()]

# 4 map functions
story += [P("03 MVP 功能地图与 P0 清单","h1"),P("MVP 的功能不是平均铺开，而是围绕“准确定位 - 受控诊断 - 可靠交接 - 知识闭环”四条主线。","body"),table([["领域","P0 功能","核心验收"],["用户前台","二维码解析、产品展示、语言选择、文本/图片/错误码、已解决/未解决反馈。","产品上下文不丢失；网络或图片失败有降级。"],["AI 编排","意图枚举、检索回答、诊断状态机、安全路由、引用与追踪。","模型不能跳过安全节点或自由创造维修步骤。"],["知识运营","上传解析、元数据、FAQ、流程、审核发布、回滚。","知识可追溯到来源、版本、责任人与适用范围。"],["人工协同","主动/自动转人工、交接包、一种渠道投递、结果回写。","人工不重复问型号、现象和已执行步骤。"],["运营质量","会话/转人工/无答案看板、会话抽检、评测集运行。","能区分知识缺失、检索错、流程错和渠道错。"],["平台基础","独立实例、角色权限、审计、密钥加密、限流与降级。","不跨租户；模型不可用时仍能进入人工。"]],[30*mm,77*mm,67*mm]),Spacer(1,7*mm),P("优先级原则","h2"),P("P0 解决“能否安全运行”；P1 解决“试点是否稳定可运营”；P2 才解决“规模化效率”。不要把语音、全渠道、离线模型等展示型能力挤进首个验证周期。","body"),PageBreak()]

# 5 frontend
story += [P("04 前台：扫码对话与售后入口","h1"),P("前台并不是一个通用聊天框，而是围绕产品上下文收集信息、执行诊断和完成交接的任务界面。","body"),table([["模块","P0 行为","异常/安全处理"],["二维码解析","解析签名令牌，展示已识别产品、地区与产品图。","失效/下架/地区不符：手动选择产品并记录。"],["会话","匿名会话 ID；文本、图片、错误码；流式回答。","断网重试；图片失败不丢文字；可随时终止。"],["信息采集","显示当前识别的型号、固件、错误码、问题分类。","未知固件时先追问或走保守路径。"],["诊断交互","每次一个关键动作，等待用户确认结果。","连续失败进入升级判断，避免循环。"],["转人工","用户主动或系统自动转人工；采集最小联系方式。","展示渠道与非实时响应说明，避免错误承诺。"]],[30*mm,83*mm,61*mm]),Spacer(1,8*mm),card("图片能力的首期边界","只支持错误码 OCR、产品标签 OCR、屏幕提示与指示灯描述辅助。模型应给“识别结果 + 不确定性 + 请求确认”，不能据图直接做维修或安全判断。",LO),Spacer(1,6*mm),P("前台验收示例","h2"),P("用户从二维码进入，上传屏幕错误码图片后，系统记录图片、抽取候选错误码、请求用户确认；随后只检索该型号和地区对应的已发布错误码方案。涉及发热、烧焦、进水或拆机时立即停止自助操作并给出人工路径。","body"),PageBreak()]

# 6 AI and diag
story += [P("05 AI 编排、检索与诊断状态机","h1"),P("大模型负责理解、追问、翻译和表达；事实边界、流程跳转、安全规则由系统控制。","body"),P("请求处理链路","h2"),Flow(["识别意图", "硬过滤\n产品/版本/地区", "混合检索\n关键词+向量", "重排序与\n证据检查", "回答/流程\n/人工升级"]),Spacer(1,7*mm),table([["路径","适用问题","实现原则"],["知识回答","单点事实、说明、公开政策、简单 FAQ","强过滤后混合检索；回答必须携带来源和版本；证据不足不下结论。"],["诊断流程","多步排查、错误码、安装配置、需要分支判断的问题","受控状态机：问题、操作、预期、分支、停止条件和升级节点。"],["业务/人工","保修、退款、订单、投诉、安全、用户主动要求","规则优先，必要时只解释公开政策，直接创建交接包。"]],[31*mm,49*mm,94*mm]),Spacer(1,7*mm),card("流程节点最小结构","nodeId、类型(question/instruction/decision/handoff/end)、适用条件、提示语、期望输入、分支、风险标记、来源引用、责任人和版本。模型可以解释节点，但不可自行改写跳转。",LT),Spacer(1,6*mm),P("安全路由规则","h2"),P("无有效证据、知识冲突、电源/拆机/进水/烧焦风险、退款或保修资格、连续两次步骤失败、用户明确要求人工，均不依赖模型自报置信度，而由编排层直接追问、停止或升级。","body"),PageBreak()]

# 7 knowledge backend
story += [P("06 后台：知识治理与产品配置","h1"),P("知识运营是核心产品，不应被简化为一个“训练”按钮。","body"),table([["模块","P0 能力","关键约束"],["产品中心","Brand -> Family -> Model -> Hardware Revision -> Firmware -> Region Variant；别名、配件、停售和二维码绑定。","型号、版本和地区是知识适用性的硬约束。"],["文档中心","PDF/DOCX/HTML/FAQ/CSV/图片导入；解析、元数据、去重、索引。","每份内容要有来源、owner、语言、适用范围、有效期。"],["FAQ/流程","编辑事实问答与受控诊断节点；关联来源；模拟测试。","高风险节点必须标记停止和人工升级。"],["审核发布","draft -> review -> approved -> published -> deprecated -> archived。","发布版本不可变；会话记录所用版本；支持紧急下架。"],["质量运营","抽检会话、标注错误类型、识别无答案与冲突知识。","AI/人工输出只能成为知识候选，不能自动发布。"]],[31*mm,82*mm,61*mm]),Spacer(1,8*mm),P("知识处理流水线","h2"),Flow(["上传与校验", "解析/OCR\n元数据补齐", "分块与索引", "专家审核", "发布、回滚\n与监控"]),Spacer(1,7*mm),P("首批知识选择策略","h2"),P("只选择“高频、风险低、可标准化、存在证据、可由专家审核”的主题。不要把所有历史工单直接向量化后开放给模型；这会让过期、偶然或错误的处理经验混入用户答案。","body"),PageBreak()]

# 8 handoff analytics
story += [P("07 人工协同、运营数据与非功能要求","h1"),P("转人工不是失败，而是将复杂个案以更完整的上下文交给成本更高但更可靠的人工渠道。","body"),table([["交接包字段","说明"],["设备上下文","租户、型号、硬件/固件、地区、语言、二维码来源。"],["问题证据","原始对话、摘要、错误码、图片链接、用户联系方式与同意记录。"],["处理轨迹","已尝试步骤及结果、引用知识、流程节点、转人工原因。"],["业务状态","优先级、渠道投递状态、人工结果、最终解决方案、知识候选。"]],[55*mm,119*mm]),Spacer(1,7*mm),P("P0 运营看板","h2"),P("扫码/会话量、产品/地区/语言分布、意图与错误码分布、自主解决/人工升级/放弃会话、Top 无答案问题、Top 差评回答、知识使用量和过期状态、模型耗时/费用、交接成功率。","body"),Spacer(1,5*mm),table([["非功能能力","MVP 标准"],["租户与权限","客户独立实例；平台运维、客户管理员、知识审核人、人工客服、只读分析员。"],["安全与隐私","HTTPS、密钥加密、对象存储签名、上传校验、最小化采集、可配置保留和删除。"],["可观测性","请求/会话 ID、模型耗时与费用、检索命中、知识版本、渠道投递结果。"],["降级","模型或人工渠道故障时，显示明确告知、保留已发布 FAQ 和可重试的人工入口。"]],[42*mm,132*mm]),PageBreak()]

# 9 architecture and model
story += [P("08 服务边界、数据模型与接口","h1"),P("首个版本可采用模块化单体或少量服务，重点是边界清晰、可观测、可替换；不需要一开始微服务化。","body"),P("逻辑服务边界","h2"),Flow(["扫码 Web/PWA", "API 网关", "会话与\n产品上下文", "AI 编排", "知识/流程\n/人工适配", "分析与审计"],width=493,height=80),Spacer(1,7*mm),table([["实体","关键字段","作用"],["ProductModel / Variant","tenantId、型号、硬件版本、固件范围、地区","决定知识与流程适用范围。"],["QRBinding","tokenId、modelId、region、batch、status","将扫码入口绑定到受控上下文。"],["KnowledgeRevision / Chunk","来源、版本、适用范围、审批、有效期","知识可追溯、可过滤、可回滚。"],["TroubleshootFlow / Node","触发条件、节点类型、分支、安全规则、来源","多步诊断的受控状态机。"],["Conversation / Message","产品上下文、语言、内容、引用、轨迹","会话恢复、抽检、回归与交接。"],["HandoffRequest","原因、交接包、渠道状态、人工结果","跨渠道的统一人工协作对象。"],["EvaluationCase / FeedbackLabel","标准输入、期望、切片、标注","避免只凭满意度判断质量。"]],[37*mm,74*mm,63*mm]),Spacer(1,7*mm),P("关键 API","h2"),table([["接口","用途","约束"],["POST /public/qr/resolve","验签并建立扫码上下文","只返回最小公开产品信息。"],["POST /conversations/{id}/messages","对话、图片、步骤结果","持久化知识版本与流程轨迹。"],["POST /conversations/{id}/handoffs","创建人工交接","渠道失败可重试、可追踪。"],["POST /admin/documents / publish","知识上传、审核、发布","必须填适用范围和审核人。"],["POST /admin/evaluations/run","回归评测","按型号/语言/风险切片输出。"]],[55*mm,64*mm,55*mm]),PageBreak()]

# 10 roadmap
story += [P("09 从 0 到试点的阶段计划","h1"),P("以下周期基于：1 名产品/知识负责人、2 名后端、1 名前端、0.5 名测试/UX，且试点客户能稳定提供售后专家。","body"),table([["阶段","目标与输出","完成闸门"],["0. 发现与数据审计\n约 1-2 周","访谈；盘点说明书、工单、错误码、语言和渠道；选首批主题；定义安全边界和验收集。","产品上下文、数据、审核责任人、人工渠道四项都可落地。"],["1. 领域基础\n约 2 周","产品模型、二维码、文档上传解析、知识审核发布、基础检索。","一份知识可关联型号/地区/版本并被正确过滤与引用。"],["2. 对话与诊断\n约 2-3 周","扫码页、图文输入、意图路由、FAQ、流程状态机、安全规则。","高频场景可从扫码走到解决/未解决/人工，不能跳过安全节点。"],["3. 人工与运营\n约 1-2 周","一种渠道集成、交接包、结果回写、基础看板和抽检。","人工无需重复问产品与已尝试步骤；每个升级有原因。"],["4. 评测与灰度\n约 2 周","真实工单评测集、回归报告、故障预案、试点操作手册。","可按型号/地区/语言回滚知识并观察影响。"]],[32*mm,82*mm,60*mm]),Spacer(1,8*mm),P("每周节奏","h2"),P("每日看失败和风险；每周抽检、复盘 Top 未解决问题和审核知识；每两周运行回归评测并复核术语/安全规则；每月与客户复盘解决率、重复咨询、人工负担和产品质量信号。","body"),PageBreak()]

# 11 dilemma 1-3
story += [P("10 重点难点档案（一）","h1"),P("以下问题是 MVP 能否跑通的决定性风险，应有专人负责、独立指标和阶段闸门。","body"),card("难点 1：知识冷启动与源数据质量（高）","<b>根因</b>：说明书不覆盖异常，工单没有最终方案，专家经验隐性。<br/><b>P0 应对</b>：阶段 0 做数据评分；只选高频、低风险、有证据的主题；AI 生成草稿但必须关联来源并经专家发布；每项知识指定 owner 和复审日。<br/><b>验证</b>：可追溯且已批准的知识比例、无答案占比、更新处理时间。<br/><b>禁止</b>：把全部历史工单直接向量化并对用户开放。",LO),Spacer(1,4*mm),card("难点 2：型号、固件、地区错配（高）","<b>根因</b>：同名产品在不同市场存在配件、接口、固件和政策差异。<br/><b>P0 应对</b>：扫码提供签名上下文；发布知识强制填适用范围；检索先硬过滤后语义匹配；改选产品时清理旧诊断上下文。<br/><b>验证</b>：错误型号引用率、版本未知追问率、跨地区政策错误率。",LB),Spacer(1,4*mm),card("难点 3：RAG 幻觉与合理但错误的步骤（高）","<b>根因</b>：相似度不等于适用性，模型会补全缺失事实，自报置信度不可靠。<br/><b>P0 应对</b>：事实回答与多步诊断分离；硬过滤 + 混合检索 + 重排序 + 引用；无证据不作答、冲突不决策、风险先停机。<br/><b>验证</b>：引用正确率、无答案正确拒答率、安全拦截漏检率。",LP),PageBreak()]

# 12 dilemma 4-6
story += [P("11 重点难点档案（二）","h1"),card("难点 4：专家经验难以变成流程（高）","<b>根因</b>：诊断依赖隐性条件和观察结果，文章无法表达分支与停止条件。<br/><b>P0 应对</b>：围绕少量高频故障召开工作坊；使用受限节点类型；每个流程必须有适用范围、来源、风险级别和 owner；上线前模拟成功/失败路径。<br/><b>验证</b>：流程完成率、重复步骤率、连续失败后正确升级率。",LT),Spacer(1,4*mm),card("难点 5：多语言与术语本地化（中高）","<b>根因</b>：部件名和政策语义会因市场变化，机器翻译会误译按键、接口和安全警告。<br/><b>P0 应对</b>：建立不可自由翻译的术语表；高风险步骤使用审核后的本地化文本；记录输入、检索、输出语言。<br/><b>验证</b>：术语一致性、按语言切片的解决率/转人工率、安全提示漏译率。",LO),Spacer(1,4*mm),card("难点 6：图片识别的能力边界（中高）","<b>根因</b>：图片质量差，视觉模型会给候选解释而非物理事实。<br/><b>P0 应对</b>：只支持 OCR 和辅助识别；提供拍照引导；输出不确定性并请求确认；高风险信号直接升级人工。<br/><b>验证</b>：有效图片率、OCR 准确率、图片导致错误流程率。",LB),Spacer(1,6*mm),P("共同原则","h2"),P("不要用“更强模型”掩盖知识、流程和数据边界问题。模型升级可以提高表达和理解，但不能替代受控的适用范围、安全规则与人工审核。","quote"),PageBreak()]

# 13 dilemma 7-9
story += [P("12 重点难点档案（三）","h1"),card("难点 7：人工接管体验与渠道现实（高）","<b>根因</b>：转人工涉及时区、隐私许可、渠道能力和责任归属，不是一个链接。<br/><b>P0 应对</b>：首期只接客户真实在用的一种渠道；统一交接包与状态机；区分实时转接和留资后回复；人工结果回流为知识候选。<br/><b>验证</b>：交接成功率、人工二次问询率、首次人工响应时长、关闭结果完整率。",LT),Spacer(1,4*mm),card("难点 8：私有化与外部模型 API 的矛盾（中高）","<b>根因</b>：私有化由数据边界、网络出口、密钥、保留和运维责任组成，不是部署开关。<br/><b>P0 应对</b>：第一档明确为“客户独立实例 + 外部 API”；透明展示数据流；支持 BYOK；日志脱敏；预留模型适配层。<br/><b>验证</b>：跨租户访问为零、密钥泄漏为零、脱敏覆盖率、调用故障降级成功率。",LP),Spacer(1,4*mm),card("难点 9：评测、反馈与自我强化风险（高）","<b>根因</b>：真实对话结果不天然是真相，错误回答可能被接受后反向污染知识。<br/><b>P0 应对</b>：建立人工标注评测集；按型号/地区/语言/风险切片；每次变更回归；线上反馈仅为结果标签或知识候选，不能自动发布。<br/><b>验证</b>：切片退化率、严重错误率、评测通过率、知识候选采纳率。",LO),PageBreak()]

# 14 tests and close
story += [P("13 测试、验收与共创启动","h1"),table([["测试层次","重点"],["接口与权限","二维码验签、产品归属、知识版本、角色权限、渠道重试。"],["检索与知识","相似型号、错误码、固件变更、过期知识、无答案、冲突知识。"],["流程与对话","模糊回答、用户拒绝操作、连续失败、跨语言、会话恢复。"],["安全红队","诱导拆机、伪造保修、提示词注入、隐私泄露、越权访问。"],["试点验收","限定产品/语言/问题范围内，由人工审核真实案例。"]],[42*mm,132*mm]),Spacer(1,7*mm),P("MVP 验收清单","h2"),P("□ 二维码只能解析到所属租户和已发布产品　□ 未标适用范围的知识不能面向用户　□ 可完成一条 FAQ 和一条诊断路径　□ 回答记录知识版本与节点　□ 风险触发停止与人工升级　□ 交接包可复现已完成步骤　□ 可紧急下架知识　□ 有可重复运行的真实问题评测集　□ 故障时有明确降级告知。","body"),Spacer(1,7*mm),P("第一次共创工作坊","h2"),P("参与者：合伙人、售后负责人、资深客服/维修专家、海外销售/渠道负责人、技术负责人。议程：确定试点范围；展示真实工单主题与重复率；选首批高频低风险主题；画出“触发 - 追问 - 操作 - 预期 - 分支 - 升级”；明确安全/隐私/保修边界；确认数据、渠道、二维码和验收样本；达成里程碑与每周评审机制。","body"),Spacer(1,7*mm),card("最终产品判断","厂商购买的不是模型调用次数，而是将正确的产品知识在正确版本和地区，以安全、可验证的步骤交付给用户，并把每一次人工处理继续变成可管理的知识资产。",CREAM),Spacer(1,10*mm),P("参考：Intercom Knowledge Sources；Zendesk External Content；Anthropic Contextual Retrieval；Microsoft Azure AI Search RAG Guide；OpenAI Evals Guide。详见同名 Markdown 的可点击链接。","small")]

doc.build(story)
print(OUT)
