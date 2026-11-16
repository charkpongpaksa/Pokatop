# ตารางทดสอบตาม rubric — Pokatop

- ทดสอบบน emulator Pixel_8 (Android API 37)
- หลักฐานอยู่ใน `docs/evidence/`
- ใช้คอลัมน์ "ข้อ rubric" กรอก checklist 1/0 ในไฟล์ `CS364_TermProject_Rubric1-69.xlsx`

## A. ฟังก์ชันของแอป (rubric 1.1, 1.2)

| ข้อ rubric | ฟังก์ชัน | วิธีทดสอบ | ผลที่คาดหวัง | ผลที่ได้ | หลักฐาน |
|---|---|---|---|---|---|
| 1.1/1.2 | คว่ำมือถือเพื่อเริ่ม | ตั้ง accelerometer เป็น 0:0:-9.8 | สถานะ FOCUSING จอดำ | ผ่าน | `t12-flow-02-facedown.png` |
| 1.1/1.2 | ช่วงผ่อนผัน 10 วิ | คว่ำแล้วหยิบ (0:9.8:0) แล้วคว่ำกลับภายใน 10 วิ | เวลายังเดิน ไม่นับเสียสมาธิ | ผ่าน (JUnit `liftWithinGraceKeepsCounting` + เครื่องจริง) | `t12-flow-03-grace.png` |
| 1.1/1.2 | หยิบเกิน 10 วิ | หยิบค้างไว้ 11 วิ | หยุดชั่วคราว เสียสมาธิ +1 เวลาตัดย้อนไปตอนหยิบ | ผ่าน | `t12-flow-04-paused.png` |
| 1.1/1.2 | ครบรอบ + เตือน | ช่วงกำหนดเอง 5 นาที คว่ำจนครบ | การ์ดเตือน เสียง สั่น แล้วเข้าพักสั้น | ผ่าน | `t12-flow-05-round-done.png`, `t12-flow-06-break.png` |
| 1.1/1.2 | พักยาวทุก N รอบ | JUnit วน 4 รอบ | รอบที่ 4 ได้พักยาว 15 นาที | ผ่าน | `FocusSessionTest.fourthRoundGivesLongBreak` |
| 1.1/1.2 | หลังพักต้องคว่ำใหม่ | มือถือคว่ำค้างจนหมดเวลาพัก | ยังไม่เริ่มรอบใหม่จนกว่าจะหยิบแล้วคว่ำ | ผ่าน | `FocusSessionTest.nextRoundNeedsFreshFlipAfterBreak` |
| 1.1/1.2 | โหมดจับเวลาธรรมดา | ปิดสวิตช์ตรวจจับ กดเริ่ม/หยุด/ทำต่อ | นับเวลาได้ ไม่นับเสียสมาธิ แสดง "—" | ผ่าน | `FocusSessionTest.plainModeStartPauseResume`, ทดสอบบนเครื่อง |
| 1.1/1.2 | เลือกช่วง Pomodoro | ตั้งค่า → เลือก สั้น / กำหนดเอง 5 นาที | หน้าจับเวลาแสดงช่วงใหม่ (ครั้งถัดไปถ้ามีการอ่านค้าง) | ผ่าน | `t12-flow-01-waiting.png` |
| 1.1/1.2 | บันทึกการอ่าน | กด "จบการอ่าน" | toast บันทึกแล้ว มีในประวัติ สถิติเพิ่ม | ผ่าน | `t12-flow-07-saved.png`, `t12-flow-08-history.png`, `t12-flow-09-stats.png` |
| 1.1/1.2 | อ่านไม่ถึง 1 นาทีไม่บันทึก | เริ่มแล้วจบทันที | toast "อ่านไม่ถึง 1 นาที จึงไม่บันทึก" | ผ่าน | `FocusSessionTest.shortSessionYieldsNoRecord` |
| 1.1/1.2 | สถิติ 7 วัน | เปิดหน้าสถิติ | การ์ด 4 ใบ + กราฟแท่ง วันนี้สีมิ้นต์ | ผ่าน | `t12-th-light-stats.png`, `DailyStatsTest` |
| 1.1/1.2 | จอคว่ำแตะไม่ได้ | แตะตำแหน่งปุ่มจบการอ่านขณะจอดำ | การอ่านไม่ถูกจบ | ผ่าน | ledger Task 6 |

## B. รองรับอุปกรณ์หลากหลาย (rubric 3)

| ข้อ rubric | วิธีทดสอบ | ผลที่คาดหวัง | ผลที่ได้ | หลักฐาน |
|---|---|---|---|---|
| 3.1 | Pixel_8 (411×914 dp) และจอเล็ก 720×1280 @320dpi (360×640 dp) ด้วย `adb shell wm size/density` | ข้อความและปุ่มไม่ถูกตัด/ซ้อน ใช้งานได้ | ผ่าน | `t12-en-light-*.png`, `t12-th-small-*.png` |
| 3.2 | หมุนเป็นแนวนอนในทุกหน้า | เนื้อหาครบ ใช้งานได้ หน้าจับเวลาใช้ layout แนวนอน สถานะไม่หาย | ผ่าน | `t12-th-land-*.png`, `MainActivityRotationTest` |
| 3.3 | ตั้งค่า → ภาษา ไทย/English | เมนู ปุ่ม toast dialog เปลี่ยนภาษา | ผ่าน | `t12-th-light-*.png` เทียบ `t12-en-light-*.png`, `t12-back-02-exit-dialog.png` |
| 3.4 | ตั้งค่า → ธีม สว่าง/มืด | ทุกหน้าเปลี่ยนธีม อ่านได้ ปุ่มแยกจากพื้นหลัง | ผ่าน | `t12-en-dark-*.png` |

## C. Back / ลบ / lifecycle (rubric 4)

| ข้อ rubric | วิธีทดสอบ | ผลที่คาดหวัง | ผลที่ได้ | หลักฐาน |
|---|---|---|---|---|
| 4.1 | กด Back จากสถิติ ประวัติ ตั้งค่า และประวัติที่เปิดจากสถิติ | กลับหน้าก่อนหน้าตามแผนภาพ | ผ่าน | report-evidence หัวข้อ 2 |
| 4.2 | หน้าจับเวลาไม่มีการอ่านค้าง กด Back ครั้งเดียว | toast ให้กดซ้ำ ยังไม่ออก | ผ่าน | `t12-back-01-toast.png` |
| 4.2 | มีการอ่านค้าง กด Back แล้วกดยกเลิก | dialog ถามก่อนออก ยกเลิกแล้วยังอยู่ในงาน | ผ่าน | `t12-back-02-exit-dialog.png`, `t12-back-03-cancel-stays.png` |
| 4.2 | กด "จบการอ่าน" ระหว่างมีการอ่านค้าง แล้วกด "อ่านต่อ" | ถามยืนยันก่อนจบ ยกเลิกแล้วยังอ่านต่อได้ | ผ่าน | `t12-end-confirm.png`, `MainActivityEndConfirmTest` |
| 4.2 | แก้ค่ากำหนดเองแล้วกด Back | dialog "ทิ้งค่าที่แก้ไข?" | ผ่าน | ทดสอบบนเครื่อง (ledger Task 7) |
| 4.3 | ลบทีละรายการ: ยกเลิก แล้ว ยืนยัน | ยกเลิกแล้วข้อมูลยังอยู่ ยืนยันแล้วลบ | ผ่าน | `t12-delete-01..03*.png`, `HistoryDeleteTest` |
| 4.3 | ลบทั้งหมด | ถามยืนยันก่อน | ผ่าน | `t12-delete-04-all-confirm.png`, `HistoryDeleteTest.deleteAllShowsEmpty` |
| 4.4 | สลับแอป/กด Home ระหว่างอ่าน แล้วกลับมา | เวลาหยุด กลับมาอ่านต่อจากเวลาเดิมได้ | ผ่าน | `t12-flow-04-paused.png`, `FocusSessionTest.returnDoesNotResumeFocus` |
| 4.4 | ระบบปิดแอปขณะอยู่เบื้องหลัง (force-stop) แล้วเปิดใหม่ | สถานะหยุดชั่วคราวพร้อมเวลาและจำนวนเสียสมาธิเดิม | ผ่าน | ledger Task 6 |
| 4.4 | เสียงตัวอย่างในตั้งค่า แล้วสลับแอป | เสียงหยุด | ผ่าน | `SettingsActivity.onPause` |

## D. ความท้าทายทางเทคนิค (rubric 2.2–2.4, 5.3)

| เรื่อง | ก่อนแก้ | หลังแก้ |
|---|---|---|
| จอดับเองเมื่อคว่ำ (FaceDownDetector ของ Android) | `t12-ch1-screenoff-before.txt` | `t12-ch1-screenoff-after.txt`, `MainActivityScreenOnTest` |
| sensor สั่น / วางเอียง | `t12-ch2-tilt-before.*`, naive 104 ครั้ง | `t12-ch2-tilt-after.*`, filtered 0 ครั้ง (`t12-ch2-jitter-counts.txt`) |
| หมุนจอถูกนับว่าออกจากแอป | `t12-ch3-rotation-before.txt` | `t12-ch3-rotation-after.txt` |

## E. ผลรวมเทสต์อัตโนมัติ

- unit test 52 กรณี (`./gradlew :app:testDebugUnitTest`) ผ่านทั้งหมด
- instrumented 13 กรณี (`./gradlew :app:connectedDebugAndroidTest`) ผ่านทั้งหมด
