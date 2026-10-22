"""Main-town selector, not an exhaustive postal locality directory.

District coverage: https://www.statistics.gov.lk/qlink/AdminDivCode
Sellers select the nearest town. Keep spelling stable for saved listings.
"""

LOCATIONS = {
    'Ampara': ['Ampara', 'Akkaraipattu', 'Kalmunai', 'Sainthamaruthu', 'Sammanthurai', 'Pottuvil'],
    'Anuradhapura': ['Anuradhapura', 'Eppawala', 'Kekirawa', 'Medawachchiya', 'Mihintale', 'Nochchiyagama', 'Tambuttegama'],
    'Badulla': ['Badulla', 'Bandarawela', 'Diyatalawa', 'Ella', 'Hali-Ela', 'Haputale', 'Mahiyanganaya', 'Passara', 'Welimada'],
    'Batticaloa': ['Batticaloa', 'Eravur', 'Kattankudy', 'Valaichchenai', 'Vakarai'],
    'Colombo': ['Colombo', 'Angoda', 'Athurugiriya', 'Avissawella', 'Battaramulla', 'Boralesgamuwa', 'Dehiwala', 'Hanwella', 'Homagama', 'Kaduwela', 'Kesbewa', 'Kohuwala', 'Kolonnawa', 'Kottawa', 'Maharagama', 'Malabe', 'Moratuwa', 'Mount Lavinia', 'Nugegoda', 'Padukka', 'Pannipitiya', 'Piliyandala', 'Rajagiriya', 'Ratmalana', 'Sri Jayawardenepura Kotte'],
    'Galle': ['Galle', 'Ambalangoda', 'Baddegama', 'Bentota', 'Elpitiya', 'Hikkaduwa', 'Karapitiya', 'Unawatuna'],
    'Gampaha': ['Gampaha', 'Attanagalla', 'Biyagama', 'Divulapitiya', 'Ganemulla', 'Ja-Ela', 'Kadawatha', 'Kandana', 'Katunayake', 'Kelaniya', 'Kiribathgoda', 'Minuwangoda', 'Mirigama', 'Negombo', 'Nittambuwa', 'Ragama', 'Veyangoda', 'Wattala'],
    'Hambantota': ['Hambantota', 'Ambalantota', 'Beliatta', 'Tangalle', 'Tissamaharama', 'Weeraketiya'],
    'Jaffna': ['Jaffna', 'Chavakachcheri', 'Chunnakam', 'Karainagar', 'Kayts', 'Kopay', 'Nallur', 'Point Pedro'],
    'Kalutara': ['Kalutara', 'Aluthgama', 'Bandaragama', 'Beruwala', 'Horana', 'Ingiriya', 'Matugama', 'Panadura', 'Wadduwa'],
    'Kandy': ['Kandy', 'Akurana', 'Ampitiya', 'Digana', 'Gampola', 'Gelioya', 'Kadugannawa', 'Katugastota', 'Kundasale', 'Nawalapitiya', 'Peradeniya', 'Pilimathalawa', 'Wattegama'],
    'Kegalle': ['Kegalle', 'Aranayaka', 'Dehiowita', 'Deraniyagala', 'Galigamuwa', 'Mawanella', 'Rambukkana', 'Ruwanwella', 'Warakapola', 'Yatiyanthota'],
    'Kilinochchi': ['Kilinochchi', 'Pallai', 'Paranthan', 'Poonakary'],
    'Kurunegala': ['Kurunegala', 'Alawwa', 'Bingiriya', 'Galgamuwa', 'Giriulla', 'Hettipola', 'Ibbagamuwa', 'Kuliyapitiya', 'Maho', 'Mawathagama', 'Narammala', 'Nikaweratiya', 'Pannala', 'Polgahawela', 'Wariyapola'],
    'Mannar': ['Mannar', 'Madhu', 'Murunkan', 'Nanattan', 'Pesalai', 'Talaimannar'],
    'Matale': ['Matale', 'Dambulla', 'Galewela', 'Naula', 'Rattota', 'Sigiriya', 'Ukuwela'],
    'Matara': ['Matara', 'Akuressa', 'Deniyaya', 'Devinuwara', 'Dickwella', 'Hakmana', 'Kamburupitiya', 'Weligama'],
    'Monaragala': ['Monaragala', 'Bibile', 'Buttala', 'Kataragama', 'Siyambalanduwa', 'Thanamalwila', 'Wellawaya'],
    'Mullaitivu': ['Mullaitivu', 'Mankulam', 'Oddusuddan', 'Puthukkudiyiruppu', 'Thunukkai'],
    'Nuwara Eliya': ['Nuwara Eliya', 'Ginigathhena', 'Hatton', 'Kotagala', 'Maskeliya', 'Nanu Oya', 'Ragala', 'Talawakelle', 'Walapane'],
    'Polonnaruwa': ['Polonnaruwa', 'Aralaganwila', 'Hingurakgoda', 'Kaduruwela', 'Medirigiriya', 'Welikanda'],
    'Puttalam': ['Puttalam', 'Anamaduwa', 'Chilaw', 'Dankotuwa', 'Kalpitiya', 'Madampe', 'Marawila', 'Nattandiya', 'Wennappuwa'],
    'Ratnapura': ['Ratnapura', 'Balangoda', 'Eheliyagoda', 'Embilipitiya', 'Godakawela', 'Kahawatta', 'Kalawana', 'Kuruwita', 'Pelmadulla'],
    'Trincomalee': ['Trincomalee', 'Kantale', 'Kinniya', 'Muttur', 'Nilaveli', 'Seruwila'],
    'Vavuniya': ['Vavuniya', 'Cheddikulam', 'Nedunkeni', 'Omanthai'],
}
