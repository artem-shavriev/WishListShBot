package GiftBot.GiftBot.service;

import GiftBot.GiftBot.config.BotConfig;
import GiftBot.GiftBot.exception.ConflictException;
import GiftBot.GiftBot.exception.NotFoundException;
import GiftBot.GiftBot.gift.Gift;
import GiftBot.GiftBot.gift.GiftRepository;
import GiftBot.GiftBot.giftBookings.GiftBookings;
import GiftBot.GiftBot.giftBookings.GiftBookingsRepository;
import GiftBot.GiftBot.user.User;
import GiftBot.GiftBot.user.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GiftBookingsRepository giftBookingsRepository;

    static final String START_COMMAND = "/start";
    static final String HELP_COMMAND = "/help";
    static final String FRIEND_COMMAND = "/choose_friend";
    static final String ADD_COMMAND = "/add_to_wish_list";
    static final String WISH_LIST_COMMAND = "/wish_list";
    static final String BOOKING_LIST_COMMAND = "/booking_list";
    static final String DELETE_GIFT_COMMAND = "/delete_gift";
    static final String DELETE_BOOKING_COMMAND = "/delete_booking";

    static final String HELP_TEXT = "WishListSHBot создан для облегчения выбора подарка для друга. \n\n" +
            "Для выбора команды можно использовать меню в левой нижней части экрана \n\n" +
            "или напечатать команду текстом: \n\n" +
            "Список доступных команд:\n\n" +
            START_COMMAND + " \n\n" +
            HELP_COMMAND + " \n\n" +
            FRIEND_COMMAND + " \n\n" +
            ADD_COMMAND + " \n\n" +
            WISH_LIST_COMMAND + " \n\n" +
            BOOKING_LIST_COMMAND + " \n\n" +
            DELETE_BOOKING_COMMAND + " \n\n" +
            DELETE_GIFT_COMMAND;

    static final String ERROR_TEXT = "Error occurred: ";
    static final String FRIEND_BUTTON = "FRIEND_BUTTON";
    static final String CREATE_LIST_BUTTON = "CREATE_LIST_BUTTON";
    static final String GET_MY_GIFTS_BUTTON = "GET_MY_GIFTS_BUTTON";
    static final String CHOOSE_GIFT = "CHOOSE_GIFT";
    static final String CHOOSE_DELETING_GIFT = "CHOOSE_DELETING_GIFT";
    static final String CHOOSE_FRIEND = "CHOOSE_FRIEND";
    static final String BOOKING_BUTTON = "BOOKING_BUTTON";
    static final String GIFT_NAME = "GIFT_NAME";
    static final String DELETE_GIFT_BUTTON = "DELETE_GIFT_BUTTON";
    static final String DELETE_BOOKING_BUTTON = "DELETE_BOOKING_BUTTON";
    static final String CHOOSE_BOOKING_FOR_DELETE = " CHOOSE_BOOKING_FOR_DELETE";
    static final String BOOKING_LIST_BUTTON = "BOOKING_LIST_BUTTON";

    String command = "";
    private Map<Long, Boolean> waitingForResponse = new HashMap<>();

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;

        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand(START_COMMAND, "Старт"));
        listOfCommands.add(new BotCommand(HELP_COMMAND, "Инструкция по использованию бота"));
        listOfCommands.add(new BotCommand(FRIEND_COMMAND, "Кнопка для выбора подарка другу"));
        listOfCommands.add(new BotCommand(ADD_COMMAND, "Добавить подарок в свой список желаний"));
        listOfCommands.add(new BotCommand(WISH_LIST_COMMAND, "Показать мой список желаний"));
        listOfCommands.add(new BotCommand(BOOKING_LIST_COMMAND, "Показать список моих броней"));
        listOfCommands.add(new BotCommand(DELETE_GIFT_COMMAND, "Удалить подарок из моего списка желаний"));
        listOfCommands.add(new BotCommand(DELETE_BOOKING_COMMAND, "Удалить бронь на подарок"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка установки меню бота. {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && botConfig.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();

                for (User user : users) {
                    prepareAndSendMessage(user.getId(), textToSend);
                }

            } else if (command.equals(GIFT_NAME)) {

                if (messageText.equals(START_COMMAND)) {
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    command = "";

                } else if (messageText.equals("отмена") || messageText.equals("Отмена")) {
                    command = "";
                    prepareAndSendMessage(chatId, "Добавление подарка в список желаний прервано. " +
                            "Выбери другую команду в меню (в левом нижнем углу).");

                } else if (messageText.split("-").length < 2) {
                    prepareAndSendMessage(chatId, "Введены некорректные данные. " +
                            "Вводите название и ссылку как в примере. Или напиши \"отмена\".");
                } else {
                    String giftName = messageText.split("-")[0];
                    String giftUrl = messageText.split("-")[1];


                    if (addGift(chatId, giftName, giftUrl)) {
                        prepareAndSendMessage(chatId, "Подарок добавлен в ваш список.");

                        command = "";
                    } else {
                        prepareAndSendMessage(chatId, "Подарок с такой ссылкой уже есть. Введи другую сслыку.");
                    }

                }
            } else {

                switch (messageText) {
                    case START_COMMAND:
                        registerUser(update.getMessage());
                        log.info("Пользователь {} зарегестрирован.", update.getMessage().getChat().getFirstName());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case HELP_COMMAND:
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case FRIEND_COMMAND:
                        getFriendList(chatId, update.getMessage().getMessageId());
                        break;
                    case ADD_COMMAND:
                        String text = "Введине название подарка и ссылку на него через дефис. \n\n" +
                                "Пример:  \"Цветы-https://flowers/\"";

                        command = GIFT_NAME;
                        prepareAndSendMessage(chatId, text);
                        break;
                    case WISH_LIST_COMMAND:
                        getMyWishList(chatId, update.getMessage().getMessageId());
                        break;

                    case BOOKING_LIST_COMMAND:
                        getBookingList(chatId, update.getMessage().getMessageId());
                        break;

                    case DELETE_GIFT_COMMAND:
                        chooseDeleteGift(chatId, update.getMessage().getMessageId());
                        break;

                    case DELETE_BOOKING_COMMAND:
                        chooseBooking(chatId, update.getMessage().getMessageId());
                        break;

                    default:
                        prepareAndSendMessage(chatId, "Извини, эта команда еще не поддерживатеся.");
                }
            }

        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals(FRIEND_BUTTON)) {
                getFriendList(chatId, messageId);

            } else if (callBackData.equals(BOOKING_LIST_BUTTON)) {
                getBookingList(chatId, messageId);

            }else if (callBackData.equals(DELETE_GIFT_BUTTON)) {
                chooseDeleteGift(chatId, messageId);

            } else if (callBackData.equals(DELETE_BOOKING_BUTTON)) {
                chooseBooking(chatId, messageId);

            } else if (callBackData.equals(GET_MY_GIFTS_BUTTON)) {
                getMyWishList(chatId, messageId);

            } else if (callBackData.equals(CREATE_LIST_BUTTON)) {
                String text = "Введине название подарка и ссылку на него через дефис. \n\n" +
                        "Пример:  \"Цветы-https://flowers/\"";

                command = GIFT_NAME;
                executeMessageEditText(text, chatId, messageId);

            } else if (callBackData.split("-")[0].equals(CHOOSE_DELETING_GIFT)) {
                String giftName = callBackData.split("-")[1];
                Gift gift = giftRepository.findByName(giftName)
                        .orElseThrow(() -> new NotFoundException("Подарок для удаления не найден."));
                giftRepository.delete(gift);

                executeMessageEditText("Подарок: " + giftName + " удален", chatId, messageId);
                log.info("Подарок: {} удален", giftName);

            } else if (callBackData.split("-")[0].equals(CHOOSE_BOOKING_FOR_DELETE)) {
                String giftName = callBackData.split("-")[1];

                Gift gift = giftRepository.findByName(giftName)
                        .orElseThrow(() -> new NotFoundException("Подарок для удаления не найден."));
                GiftBookings booking = giftBookingsRepository.findByGiftId(gift.getId());

                giftBookingsRepository.delete(booking);

                gift.setAvailable(true);
                giftRepository.save(gift);

                executeMessageEditText("Бронь на подарок: " + giftName + " удалена", chatId, messageId);
                log.info("Бронь на подарок {} удалена", giftName);

            } else if (callBackData.split("-")[0].equals(CHOOSE_FRIEND)) {

                String friendUserName = callBackData.split("-")[1];
                List<Gift> gifts = getAvailableFriendsGifts(friendUserName);

                if (gifts.isEmpty()) {
                    prepareAndSendMessage(chatId, "Доступных подарков сейчас нет.");
                } else {
                    chooseGiftsButton(gifts, chatId, friendUserName);
                }


            } else if (callBackData.split("-")[0].equals(CHOOSE_GIFT)) {

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Для просмотра подарка по ссылке нажми на правую кнопу \n" +
                        "Для брони этого подарка нажми на левую кнопку");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

                List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                var bookingButton = new InlineKeyboardButton();
                bookingButton.setText("Забронировать");
                bookingButton.setCallbackData(BOOKING_BUTTON + "-" + callBackData);

                var linkButton = new InlineKeyboardButton();

                linkButton.setText("Ссылка на подарок");

                String url = giftRepository.findByName(callBackData.split("-")[1]).get().getLink();
                linkButton.setUrl(url);

                rowInLine.add(bookingButton);
                rowInLine.add(linkButton);

                rowsInLine.add(rowInLine);

                inlineKeyboardMarkup.setKeyboard(rowsInLine);

                message.setReplyMarkup(inlineKeyboardMarkup);

                executeMessage(message);

            } else if (callBackData.split("-")[0].equals(BOOKING_BUTTON)) {

                String friendName = callBackData.split("-")[3];
                User friend = userRepository.findByUserName(friendName)
                        .orElseThrow(() -> new NotFoundException("Пользователь не найден по userName"));

                Gift gift = giftRepository.findByName(callBackData.split("-")[2])
                        .orElseThrow(() -> new NotFoundException("Подарок не найден по иимени"));

                gift.setAvailable(false);

                giftRepository.save(gift);
                GiftBookings giftBooking = new GiftBookings();
                giftBooking.setFriendId(friend.getId());
                giftBooking.setUserId(chatId);
                giftBooking.setGiftId(gift.getId());

                giftBookingsRepository.save(giftBooking);

                String text = "Подарок: " + gift.getName() + " забронирован за вами.";

                executeMessageEditText(text, chatId, messageId);
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Привет " + name + "!" + " :blush: \n\n" +
                ":gift: В этом боте ты можешь:"
        );

        log.info("Ответ пользователю {}", name);

        startMessage(chatId, answer);
    }

    private void startMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLineCreateListButton = new ArrayList<>();
        List<InlineKeyboardButton> rowInLineFriendButton = new ArrayList<>();
        List<InlineKeyboardButton> rowInLineGetMyGifts = new ArrayList<>();
        List<InlineKeyboardButton> rowInLineDeleteGift = new ArrayList<>();
        List<InlineKeyboardButton> rowInLineDeleteBooking = new ArrayList<>();
        List<InlineKeyboardButton> rowInLineBookingList = new ArrayList<>();

        var friendButton = new InlineKeyboardButton();
        friendButton.setText("Выбрать подарок другу");
        friendButton.setCallbackData(FRIEND_BUTTON);

        var createListButton = new InlineKeyboardButton();
        createListButton.setText("Добавить подарок себе в список");
        createListButton.setCallbackData(CREATE_LIST_BUTTON);

        var createGetMyGiftsButton = new InlineKeyboardButton();
        createGetMyGiftsButton.setText("Посмотреть свой список желаний");
        createGetMyGiftsButton.setCallbackData(GET_MY_GIFTS_BUTTON);

        var createBookingListButton = new InlineKeyboardButton();
        createBookingListButton.setText("Посмотреть список моих броней");
        createBookingListButton.setCallbackData(BOOKING_LIST_BUTTON);

        var createDeleteGiftButton = new InlineKeyboardButton();
        createDeleteGiftButton.setText("Удалить подарок из списка желаний");
        createDeleteGiftButton.setCallbackData(DELETE_GIFT_BUTTON);

        var createDeleteBookingButton = new InlineKeyboardButton();
        createDeleteBookingButton.setText("Удалить бронь на подарок");
        createDeleteBookingButton.setCallbackData(DELETE_BOOKING_BUTTON);

        rowInLineFriendButton.add(friendButton);
        rowInLineCreateListButton.add(createListButton);
        rowInLineGetMyGifts.add(createGetMyGiftsButton);
        rowInLineBookingList.add(createBookingListButton);
        rowInLineDeleteGift.add(createDeleteGiftButton);
        rowInLineDeleteBooking.add(createDeleteBookingButton);

        rowsInLine.add(rowInLineFriendButton);
        rowsInLine.add(rowInLineCreateListButton);
        rowsInLine.add(rowInLineGetMyGifts);
        rowsInLine.add(rowInLineBookingList);
        rowsInLine.add(rowInLineDeleteGift);
        rowsInLine.add(rowInLineDeleteBooking);

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    private void chooseBooking(long chatId, long messageId) {
        User user = userRepository.findById(chatId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<GiftBookings> bookings = giftBookingsRepository.findByUserId(user.getId());

        if (bookings.isEmpty()) {
            log.info("Cписок броней для пользователья {} пуст", user.getUserName());
            executeMessageEditText("Ты еще не бронировал подарок другу.", chatId, messageId);
        } else {
            String text = "Выбери забронированный подарок для отмены брони на него";

            List<Long> bookingGiftsId = bookings.stream().map(b -> b.getGiftId()).toList();

            List<String> giftsNameList = giftRepository.findAllByIdList(bookingGiftsId);

            InlineKeyboardMarkup prepareInlineKeyboardMarkup = keyboardButtonsList(giftsNameList,
                    CHOOSE_BOOKING_FOR_DELETE);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);

            message.setReplyMarkup(prepareInlineKeyboardMarkup);
            executeMessage(message);
        }
    }

    private void chooseDeleteGift(long chatId, long messageId) {
        User user = userRepository.findById(chatId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Gift> gifts = userRepository.findUsersGiftsByAvailable(true, user.getUserName());

        if (gifts.isEmpty()) {
            log.info("Cписок желаний пуст");
            executeMessageEditText("Твой список желаний пуст или все подарки забронированы.", chatId, messageId);
        } else {
            String text = "Выбери подарок для удаления:";

            List<String> giftNameList = gifts.stream().map(Gift::getName).toList();

            InlineKeyboardMarkup prepareInlineKeyboardMarkup = keyboardButtonsList(giftNameList, CHOOSE_DELETING_GIFT);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);

            message.setReplyMarkup(prepareInlineKeyboardMarkup);
            executeMessage(message);
        }
    }

    private void getMyWishList(long chatId, long messageId) {
        User user = userRepository.findById(chatId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Gift> gifts = user.getUsersGifts();
        if (gifts.isEmpty()) {
            log.info("Cписок желаний пуст");
            executeMessageEditText("Ваш список желаний пуст", chatId, messageId);
        } else {
            List<String> giftsNameList = gifts.stream().map(Gift::getName).toList();
            executeMessageEditText(giftsNameList, chatId);
        }
    }

    private void getBookingList(long chatId, long messageId) {
        User user = userRepository.findById(chatId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<GiftBookings> bookings = giftBookingsRepository.findByUserId(user.getId());

        if (bookings.isEmpty()) {
            log.info("Cписок броней для пользователья {} пуст", user.getUserName());
            executeMessageEditText("Ты еще не бронировал подарок другу.", chatId, messageId);
        } else {
            List<Long> bookingGiftsId = bookings.stream().map(b -> b.getGiftId()).toList();

            List<String> giftsNameList = giftRepository.findAllByIdList(bookingGiftsId);

            executeMessageEditText(giftsNameList, chatId);
        }
    }

    private void getFriendList(long chatId, long messageId) {
        List<User> friends = userRepository.findAllByIdNot(chatId).get();

        if (friends.isEmpty()) {
            log.info("Список друзей пуст;");
            executeMessageEditText("Список друзей пуст", chatId, messageId);
        } else {
            String text = "Выбери друга из списка:";

            List<String> friendsList = new ArrayList<>();

            friends.stream().forEach(f -> friendsList.add(f.getUserName()));

            InlineKeyboardMarkup prepareInlineKeyboardMarkup = keyboardButtonsList(friendsList, CHOOSE_FRIEND);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);

            message.setReplyMarkup(prepareInlineKeyboardMarkup);
            executeMessage(message);
        }
    }

    private boolean addGift(long chatId, String giftName, String giftUrl) {
        Gift gift = new Gift();

        User user = userRepository.findById(chatId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Gift> usersGifts = user.getUsersGifts();

        List<String> giftsUrlList = usersGifts.stream().map(Gift::getLink).toList();

        if (giftsUrlList.contains(giftUrl)) {

            return false;

        } else {

            gift.setAvailable(true);
            gift.setName(giftName);
            gift.setLink(giftUrl);

            giftRepository.save(gift);

            usersGifts.add(gift);

            userRepository.save(user);

            log.info("Подарок добавлен в список.");

            return true;
        }
    }

    private List<Gift> getAvailableFriendsGifts(String friendUserName) {
        return userRepository.findUsersGiftsByAvailable(true, friendUserName);
    }

    private void chooseGiftsButton(List<Gift> gifts, long chatId, String friendUserName) {

        String text = "Нажми на один из подарков в спискке для выбора.";

        Map<String, String> giftsWithLink = new HashMap<>();
        gifts.forEach(g -> {
            giftsWithLink.put(g.getName(), g.getLink());
        });

        InlineKeyboardMarkup prepareInlineKeyboardMarkup = keyboardButtonForGiftsCreator(giftsWithLink, friendUserName);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        message.setReplyMarkup(prepareInlineKeyboardMarkup);
        executeMessage(message);
    }

    private InlineKeyboardMarkup keyboardButtonsList(List<String> buttonsList, String callbackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        for (String buttonName : buttonsList) {
            var button = new InlineKeyboardButton();

            button.setText(buttonName);
            button.setCallbackData(callbackData + "-" + buttonName);

            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            rowInLine.add(button);

            rowsInLine.add(rowInLine);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup keyboardButtonForGiftsCreator(Map<String, String> buttonsWithLink, String friendUserName) {

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        for (String giftName : buttonsWithLink.keySet()) {
            var button = new InlineKeyboardButton();

            button.setText(giftName);
            button.setCallbackData(CHOOSE_GIFT + "-" + giftName + "-" + friendUserName);

            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            rowInLine.add(button);

            rowsInLine.add(rowInLine);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInLine);

        return inlineKeyboardMarkup;
    }

    private void prepareAndSendMessage(Long chatId, String textToSend) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void registerUser(Message message) {

        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();

            user.setId(chatId);
            user.setName(chat.getFirstName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: {}", user);
        }
    }

    private void executeMessageEditText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessageEditText(List<String> listOfText, long chatId) {
        SendMessage message = new SendMessage();

        StringBuilder text = new StringBuilder();
        listOfText.forEach(t -> text.append("- ").append(t).append("\n")
                .append("------------------------------").append("\n"));

        message.setChatId(chatId);
        message.setText(String.valueOf(text));
        executeMessage(message);
    }
}
