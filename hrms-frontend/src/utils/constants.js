/**
 * 全局业务常量
 * 集中在这里，避免同一个数字散落在多个页面各写一遍。
 */

/**
 * 试用期到期提前提醒天数（含第 N 天，且包含所有已逾期记录）
 *
 * 修改这里即可同时生效于：
 *   1. 侧边栏「招聘管理 → 转正管理」角标数量
 *   2. 转正管理页顶部橙色提示条文案与数字
 *   3. 「30 天内到期」开关的筛选条件
 *   4. 剩余天数标签的变色阈值
 *
 * 注意：后端 ProbationServiceImpl.statistics() 的同名常量与之独立，
 * 因为接口会把这个天数作为参数接收，改完这里后端会跟着走，
 * 但后端自己的默认值（未传参时）也是 30，两边保持一致即可。
 */
export const PROBATION_EXPIRE_DAYS = 30

/**
 * 单次延长试用期的上限（天）
 * 与后端 ProbationServiceImpl.EXTEND_MAX_DAYS 保持一致，仅用于前端提前拦截，
 * 真正的强校验在后端。
 */
export const PROBATION_EXTEND_MAX_DAYS = 30

/**
 * 累计试用期上限（月）
 * 《劳动合同法》第十九条：三年以上固定期限和无固定期限的劳动合同，试用期不得超过六个月。
 */
export const PROBATION_TOTAL_MAX_MONTHS = 6

/**
 * 合同 / 签证临期提醒天数
 *
 * 修改这里即可同时生效于：
 *   1. 侧边栏「人事管理 → 到期提醒」角标数量
 *   2. 到期提醒页默认查询范围
 *
 * 后端接口 /contract/expiring 通过 days 参数接收，未传参时默认 60，
 * 两边保持一致即可。
 */
export const CONTRACT_EXPIRE_DAYS = 60
