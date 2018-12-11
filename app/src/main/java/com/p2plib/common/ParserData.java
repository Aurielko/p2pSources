package com.p2plib.common;

import android.app.Activity;

import com.p2plib.PayLib;
import com.p2plib.ussd.USSDController;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ParserData {
//    static HashMap mapUssd = new HashMap<>();
//
//    /*
//     * text 1
//     * **/
//    public static void parseXml() {
//        try {
//            // Создается построитель документа
//            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            // Создается дерево DOM документа из файла
//            Document document = documentBuilder.parse("com/p2plib/setting.xml");
//
//            // Получаем корневой элемент
//            Node root = document.getDocumentElement();
//            // Просматриваем все подэлементы корневого - т.е. книги
//            NodeList books = root.getChildNodes();
//            for (int i = 0; i < books.getLength(); i++) {
//                Node book = books.item(i);
//                // Если нода не текст, то это книга - заходим внутрь
//                if (book.getNodeType() != Node.TEXT_NODE) {
//                    NodeList bookProps = book.getChildNodes();
//                    for (int j = 0; j < bookProps.getLength(); j++) {
//                        Node bookProp = bookProps.item(j);
//                        // Если нода не текст, то это один из параметров книги - печатаем
//                        if (bookProp.getNodeType() != Node.TEXT_NODE) {
//                            System.out.println(bookProp.getNodeName() + ":" + bookProp.getChildNodes().item(0).getTextContent());
//                        }
//                    }
//                    System.out.println("===========>>>>");
//                }
//            }
//
//        } catch (ParserConfigurationException ex) {
//            ex.printStackTrace(System.out);
//        } catch (SAXException ex) {
//            ex.printStackTrace(System.out);
//        } catch (IOException ex) {
//            ex.printStackTrace(System.out);
//        }
//    }
//
//    public void sendUssdRoute(final String destOper, Activity act, final String ussdNum, final String sum, final String target) {
//        final USSDController ussdController = USSDController.getInstance(act);
//        ussdController.cleanCallbackMessage();
//        mapUssd.put("KEY_LOGIN", new HashSet<>(Arrays.asList("подождите", "загрузка")));
//        mapUssd.put("KEY_ERROR", new HashSet<>(Arrays.asList("problema", "problem", "ошибка", "null")));
//
//
//        ussdController.callUSSDInvoke(ussdNum, mapUssd, new USSDController.CallbackInvoke() {
//            @Override
//            public void responseInvoke(String message) {
//                //   Logger.lg("Case 1 " + message);
//                if (message.contains("1>Мобильный телефон")) {
//
//                    ussdController.send("1", new USSDController.CallbackMessage() {
//                        @Override
//                        public void responseMessage(String message) {
//                            if (message.contains("1>Оплатить МТС") && destOper == "MTS") {
//
//                                ussdController.send("1", new USSDController.CallbackMessage() {
//                                    @Override
//                                    public void responseMessage(String message) {
//                                    }
//                                });
//                            }
//                            if (destOper == "Beeline") {
//                                ussdController.send("2", new USSDController.CallbackMessage() {
//                                    @Override
//                                    public void responseMessage(String message) {
//
//                                    }
//                                });
//                            }
//                            if (destOper == "Megafon") {
//                                ussdController.send("3", new USSDController.CallbackMessage() {
//                                    @Override
//                                    public void responseMessage(String message) {
//
//                                    }
//                                });
//                            }
//                            if (destOper == "Tele2") {
//                                ussdController.send("4", new USSDController.CallbackMessage() {
//                                    @Override
//                                    public void responseMessage(String message) {
//
//                                    }
//                                });
//                            }
//                        }
//                    });
//                }
//                if (destOper == "MTS") {
//                    if (message.contains("2>Другой номер МТС")) {
//
//                        ussdController.send("2", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("Номер телефона")) {
//
//                                    ussdController.send(target, new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                    if (message.contains("Сумма платежа")) {
//
//                        ussdController.send(sum, new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("счет МТС")) {
//
//                                    ussdController.send("1", new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                    if (message.contains("счет МТС")) {
//
//                        ussdController.send("1", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//
//                            }
//                        });
//                    }
//                    if (message.contains("Сумма к оплате")) {
//
//                        ussdController.send("1", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                            }
//                        });
//                    }
//
//                }
//                if (destOper == "Beeline") {
//                    if (message.contains("Номер телефона")) {
//                        ussdController.send(target, new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("Сумма платежа")) {
//
//                                    ussdController.send(sum, new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                    if (message.contains("счет МТС")) {
//
//                        ussdController.send("1", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("Комиссия") || message.contains("Сумма к оплате")) {
//
//                                    ussdController.send("1", new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                }
//                if (destOper == "Megafon") {
//                    if (message.contains("Номер телефона")) {
//
//                        ussdController.send(target, new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("Сумма платежа")) {
//
//                                    ussdController.send(sum, new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                    if (message.contains("счет МТС")) {
//
//                        ussdController.send("1", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("Сумма к")) {
//
//                                    ussdController.send("1", new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                }
//                if (destOper == "Tele2") {
//                    if (message.toLowerCase().contains("tele2")) {
//
//                        ussdController.send("1", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("Номер телефона")) {
//
//                                    ussdController.send(target, new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                    if (message.contains("Сумма") && !message.contains("Комиссия")) {
//
//                        ussdController.send(sum, new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//                                if (message.contains("счет МТС")) {
//
//                                    ussdController.send("1", new USSDController.CallbackMessage() {
//                                        @Override
//                                        public void responseMessage(String message) {
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                    if (message.contains("Комиссия")) {
//
//                        ussdController.send("1", new USSDController.CallbackMessage() {
//                            @Override
//                            public void responseMessage(String message) {
//
//                            }
//                        });
//                    }
//                }
//            }
//
//            @Override
//            public void over(String message) {
//                PayLib.flagok = false;
//                // message has the response string data from USSD
//                // response no have input text, NOT SEND ANY DATA
//            }
//        });
//    }
}
