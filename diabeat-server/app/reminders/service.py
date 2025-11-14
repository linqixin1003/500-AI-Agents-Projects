"""智能提醒服务"""
from typing import Optional
from datetime import datetime, timedelta
from app.database import database
import logging

logger = logging.getLogger(__name__)


class SmartReminderService:
    """智能提醒服务 - 根据用户的饮食和用药习惯提供智能提醒"""
    
    # 糖尿病患者建议用餐间隔（小时）
    MEAL_INTERVAL_HOURS = 3.5
    
    # 不同用药类型的建议间隔（小时）
    MEDICATION_INTERVALS = {
        "insulin": {
            "short_acting": 4,    # 短效胰岛素
            "long_acting": 24,    # 长效胰岛素
            "default": 8          # 默认
        },
        "oral_medication": {
            "default": 12         # 口服药一般一天2次
        }
    }
    
    async def get_smart_reminders(self, user_id: str) -> dict:
        """获取智能提醒"""
        try:
            # 1. 获取最后一餐时间
            last_meal_time = await self._get_last_meal_time(user_id)
            
            # 2. 获取最后用药时间
            last_medication_time = await self._get_last_medication_time(user_id)
            last_medication_type = await self._get_last_medication_type(user_id)
            
            # 3. 预测下次用餐时间
            next_meal_time = None
            meal_reminder_message = None
            should_eat_soon = False
            
            if last_meal_time:
                hours_since_meal = (datetime.utcnow() - last_meal_time).total_seconds() / 3600
                next_meal_time = last_meal_time + timedelta(hours=self.MEAL_INTERVAL_HOURS)
                
                if hours_since_meal >= self.MEAL_INTERVAL_HOURS:
                    should_eat_soon = True
                    meal_reminder_message = f"距离上次用餐已{hours_since_meal:.1f}小时，建议少吃多餐，及时补充能量"
                elif hours_since_meal >= (self.MEAL_INTERVAL_HOURS - 0.5):
                    meal_reminder_message = f"即将到用餐时间，准备好健康的糖尿病友好食物"
                else:
                    remaining_hours = self.MEAL_INTERVAL_HOURS - hours_since_meal
                    meal_reminder_message = f"下次建议用餐时间：{remaining_hours:.1f}小时后"
            else:
                should_eat_soon = True
                meal_reminder_message = "今天还没有用餐记录，记得少吃多餐哦"
            
            # 4. 预测下次用药时间
            next_medication_time = None
            medication_reminder_message = None
            should_take_medication_soon = False
            
            if last_medication_time and last_medication_type:
                # 获取该类型药物的建议间隔
                if last_medication_type == "insulin":
                    interval_hours = self.MEDICATION_INTERVALS["insulin"]["default"]
                elif last_medication_type == "oral_medication":
                    interval_hours = self.MEDICATION_INTERVALS["oral_medication"]["default"]
                else:
                    interval_hours = 8  # 默认间隔
                
                hours_since_medication = (datetime.utcnow() - last_medication_time).total_seconds() / 3600
                next_medication_time = last_medication_time + timedelta(hours=interval_hours)
                
                if hours_since_medication >= interval_hours:
                    should_take_medication_soon = True
                    medication_reminder_message = f"距离上次用药已{hours_since_medication:.1f}小时，请按时用药"
                elif hours_since_medication >= (interval_hours - 1):
                    medication_reminder_message = f"即将到用药时间，请提前准备药物"
                else:
                    remaining_hours = interval_hours - hours_since_medication
                    medication_reminder_message = f"下次建议用药时间：{remaining_hours:.1f}小时后"
            else:
                medication_reminder_message = "暂无用药记录，请咨询医生制定用药方案"
            
            # 5. 生成提醒依据
            reasoning_parts = []
            
            if last_meal_time:
                reasoning_parts.append(f"基于上次用餐时间（{hours_since_meal:.1f}小时前）")
            else:
                reasoning_parts.append("今日尚无用餐记录")
            
            if last_medication_time:
                reasoning_parts.append(f"基于上次用药时间（{hours_since_medication:.1f}小时前）")
            else:
                reasoning_parts.append("今日尚无用药记录")
            
            reasoning_parts.append("建议糖尿病患者少吃多餐，每3-4小时进食一次")
            reasoning_parts.append("按时用药有助于更好地控制血糖")
            
            reasoning = "，".join(reasoning_parts)
            
            return {
                "next_meal_time": next_meal_time.isoformat() if next_meal_time else None,
                "next_medication_time": next_medication_time.isoformat() if next_medication_time else None,
                "meal_reminder_message": meal_reminder_message,
                "medication_reminder_message": medication_reminder_message,
                "should_eat_soon": should_eat_soon,
                "should_take_medication_soon": should_take_medication_soon,
                "reasoning": reasoning
            }
            
        except Exception as e:
            logger.error(f"获取智能提醒失败: {str(e)}", exc_info=True)
            return {
                "next_meal_time": None,
                "next_medication_time": None,
                "meal_reminder_message": "暂无数据",
                "medication_reminder_message": "暂无数据",
                "should_eat_soon": False,
                "should_take_medication_soon": False,
                "reasoning": "数据不足，无法生成提醒"
            }
    
    async def _get_last_meal_time(self, user_id: str) -> Optional[datetime]:
        """获取最后一餐时间"""
        query = """
            SELECT meal_time
            FROM meal_records
            WHERE user_id = :user_id
            ORDER BY meal_time DESC
            LIMIT 1
        """
        result = await database.fetch_one(query=query, values={"user_id": user_id})
        return result["meal_time"] if result else None
    
    async def _get_last_medication_time(self, user_id: str) -> Optional[datetime]:
        """获取最后用药时间"""
        query = """
            SELECT medication_time
            FROM medication_records
            WHERE user_id = :user_id
            ORDER BY medication_time DESC
            LIMIT 1
        """
        result = await database.fetch_one(query=query, values={"user_id": user_id})
        return result["medication_time"] if result else None
    
    async def _get_last_medication_type(self, user_id: str) -> Optional[str]:
        """获取最后用药类型"""
        query = """
            SELECT medication_type
            FROM medication_records
            WHERE user_id = :user_id
            ORDER BY medication_time DESC
            LIMIT 1
        """
        result = await database.fetch_one(query=query, values={"user_id": user_id})
        return result["medication_type"] if result else None


# 单例服务
_service_instance = None


def get_reminder_service() -> SmartReminderService:
    """获取提醒服务实例"""
    global _service_instance
    if _service_instance is None:
        _service_instance = SmartReminderService()
    return _service_instance

