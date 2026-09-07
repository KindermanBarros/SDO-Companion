package com.kinderman.sdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kinderman.sdo.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Ink = Color(0xFF171411); private val Paper = Color(0xFFF3EBDD); private val Gold = Color(0xFFB98745); private val Wine = Color(0xFF702F35)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SdoTheme { SdoApp(this) } } }
}

@Composable fun SdoTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = lightColorScheme(primary = Wine, secondary = Gold, background = Paper, surface = Color(0xFFFFF9EE), onBackground = Ink), typography = Typography(headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold), titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)), content = content)

class AppViewModel(private val repo: CharacterRepository) : ViewModel() {
    private val _uid = MutableStateFlow("demo-player")
    private val _master = MutableStateFlow(false)
    val isMaster = _master.asStateFlow()
    val characters = combine(_uid, _master) { u, m -> u to m }.flatMapLatest { repo.observe(it.first, it.second) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun setSession(uid: String, master: Boolean) { _master.value = master; _uid.value = uid; viewModelScope.launch { repo.sync(uid, master) } }
    fun add() = viewModelScope.launch { repo.save(CharacterEntity(ownerId = _uid.value)) }
    fun save(c: CharacterEntity) = viewModelScope.launch { repo.save(c) }
}

data class AuthUiState(val loading:Boolean=false,val error:String?=null)
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    val session = auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    var state by mutableStateOf(AuthUiState()); private set
    val configured get() = auth.configured
    fun submitGoogle(activity: android.app.Activity)=viewModelScope.launch { state=AuthUiState(true); state=runCatching { auth.loginWithGoogle(activity) }.fold({AuthUiState()},{AuthUiState(error=it.localizedMessage ?: "Não foi possível entrar com Google")}) }
    fun logout()=auth.logout()
    suspend fun master(uid:String)=runCatching { auth.isMaster(uid) }.getOrDefault(false)
}

@Composable fun SdoApp(activity: MainActivity) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SdoApplication
    val vm: AppViewModel = viewModel(factory = object: ViewModelProvider.Factory { override fun <T: ViewModel> create(modelClass: Class<T>): T = AppViewModel(app.repository) as T })
    val authVm: AuthViewModel = viewModel(factory = object: ViewModelProvider.Factory { override fun <T: ViewModel> create(modelClass: Class<T>): T = AuthViewModel(AuthRepository()) as T })
    val user by authVm.session.collectAsStateWithLifecycle()
    var demo by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(user?.uid, demo) { if(demo) vm.setSession("demo-player", false) else user?.let { vm.setSession(it.uid, authVm.master(it.uid)) } }
    if(user==null && !demo) { LoginScreen(activity, authVm, allowDemo=!authVm.configured){demo=true}; return }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val chars by vm.characters.collectAsStateWithLifecycle(); val master by vm.isMaster.collectAsStateWithLifecycle()
    if (selected == null) Dashboard(chars, master, vm::add) { selected = it }
    else CharacterSheet(chars.firstOrNull { it.id == selected }, master, { selected = null }, vm::save)
}

@Composable fun LoginScreen(activity: MainActivity,vm:AuthViewModel,allowDemo:Boolean,onDemo:()->Unit){Box(Modifier.fillMaxSize().background(Ink).padding(24.dp),contentAlignment=Alignment.Center){Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Paper)){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("SDO",color=Gold,fontWeight=FontWeight.Bold);Text("COMPANION",style=MaterialTheme.typography.headlineLarge);Text("Entre com sua conta Google para abrir suas histórias e sincronizar suas fichas.");vm.state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button({vm.submitGoogle(activity)},Modifier.fillMaxWidth(),enabled=!vm.state.loading){if(vm.state.loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else{Icon(Icons.Default.AccountCircle,null);Spacer(Modifier.width(8.dp));Text("Entrar com Google")}};if(allowDemo){HorizontalDivider();OutlinedButton(onDemo,Modifier.fillMaxWidth()){Text("Usar modo local de demonstração")}}}}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Dashboard(chars: List<CharacterEntity>, master: Boolean, onAdd: ()->Unit, onOpen:(String)->Unit) {
    Scaffold(containerColor = Paper, floatingActionButton = { FloatingActionButton(onClick=onAdd, containerColor=Wine, contentColor=Color.White){ Icon(Icons.Default.Add,"Criar personagem") } }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("SDO COMPANION", color=Gold, style=MaterialTheme.typography.labelLarge); Text(if(master) "Painel da Mestre" else "Suas histórias", style=MaterialTheme.typography.headlineLarge); Text("Fichas sincronizadas, disponíveis mesmo quando o mundo fica sem sinal.") }
            item { AssistChip(onClick={}, label={Text(if(master) "Acesso de Mestre" else "Acesso de Jogador")}, leadingIcon={Icon(if(master) Icons.Default.AdminPanelSettings else Icons.Default.Person,null)}) }
            if(chars.isEmpty()) item { ParchmentCard { Text("Nenhuma ficha por aqui", style=MaterialTheme.typography.titleLarge); Text("Crie o primeiro personagem no botão +.") } }
            items(chars, key={it.id}) { c -> ParchmentCard(onClick={onOpen(c.id)}) { Row(verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(52.dp).background(Wine, RoundedCornerShape(14.dp)), contentAlignment=Alignment.Center){Text(c.name.take(1),color=Color.White,style=MaterialTheme.typography.headlineSmall)}; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)){Text(c.name,style=MaterialTheme.typography.titleLarge);Text(listOf(c.race,c.occupation,"Nível ${c.level}").filter{it.isNotBlank()}.joinToString(" • "))}; Icon(Icons.Default.ChevronRight,null) } } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CharacterSheet(character: CharacterEntity?, master:Boolean, back:()->Unit, save:(CharacterEntity)->Unit) {
    if(character==null) return
    var c by remember(character.id, character.updatedAt) { mutableStateOf(character) }
    Scaffold(containerColor=Paper, topBar={TopAppBar(title={Column{Text(c.name);Text(if(master) "Visão da Mestre" else "Sua ficha",style=MaterialTheme.typography.labelSmall)}},navigationIcon={IconButton(back){Icon(Icons.Default.ArrowBack,"Voltar")}},actions={IconButton({save(c)}){Icon(Icons.Default.Save,"Salvar")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Ink,titleContentColor=Color.White,navigationIconContentColor=Color.White,actionIconContentColor=Gold))}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item { ParchmentCard { Text("IDENTIDADE",color=Gold,fontWeight=FontWeight.Bold); SheetField("Nome",c.name){c=c.copy(name=it)}; Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){SheetField("Raça",c.race){c=c.copy(race=it)}};Box(Modifier.weight(1f)){SheetField("Sub-raça",c.subRace){c=c.copy(subRace=it)}}}; SheetField("Ocupação",c.occupation){c=c.copy(occupation=it)} } }
            item { SectionTitle("Recursos"); ResourceGrid(c){c=it} }
            item { SectionTitle("Atributos & conhecimentos") }
            items(c.attributes.indices.toList()) { index -> AttributeCard(c.attributes[index]) { a -> c=c.copy(attributes=c.attributes.toMutableList().also{it[index]=a}) } }
            item { ParchmentCard { SectionTitle("Proteções"); c.protections.forEach{(name,value)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text(value.toString(),fontWeight=FontWeight.Bold,color=Wine)}} } }
            item { ParchmentCard { SectionTitle("Caminho"); SheetField("Nome do caminho",c.pathName){c=c.copy(pathName=it)}; SheetField("Lema",c.pathMotto){c=c.copy(pathMotto=it)} } }
            item { ParchmentCard { SectionTitle("História"); SheetField("De onde veio seu personagem?",c.story,true){c=c.copy(story=it)}; SectionTitle("Anotações"); SheetField("Anotações da mesa",c.notes,true){c=c.copy(notes=it)} } }
            item { Button({save(c)},Modifier.fillMaxWidth()){Icon(Icons.Default.CloudUpload,null);Spacer(Modifier.width(8.dp));Text("Salvar e sincronizar")}; Text("A cópia local é salva primeiro; a nuvem sincroniza quando houver conexão.",style=MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun ResourceGrid(c:CharacterEntity,onChange:(CharacterEntity)->Unit){ val entries=listOf("Vida" to c.life,"Sanidade" to c.sanity,"Arcano" to c.arcane,"Energia" to c.energy,"Destino" to c.destiny,"Exaustão" to c.exhaustion,"Corrupção" to c.corruption); Column(verticalArrangement=Arrangement.spacedBy(8.dp)){entries.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(n,v)->Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=if(n=="Vida") Wine else Color(0xFFFFF9EE))){Column(Modifier.padding(12.dp)){Text(n,color=if(n=="Vida") Color.White else Gold);Text("${v.current} / ${v.maximum}",style=MaterialTheme.typography.titleLarge,color=if(n=="Vida") Color.White else Ink)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}}}
@Composable private fun AttributeCard(a:AttributeValue,onChange:(AttributeValue)->Unit)=ParchmentCard{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).background(Ink,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Text(a.acronym,color=Gold,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(a.name,style=MaterialTheme.typography.titleLarge);Text("Valor ${a.value}  •  Mod ${a.modifier}")}};HorizontalDivider(Modifier.padding(vertical=10.dp));a.skills.forEach{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(it.name);Text("${it.value}  ${if(it.modifier>=0) "+" else ""}${it.modifier}",fontWeight=FontWeight.SemiBold)}}}
@Composable private fun SheetField(label:String,value:String,multiline:Boolean=false,onValue:(String)->Unit){OutlinedTextField(value,onValue,Modifier.fillMaxWidth().padding(top=6.dp),label={Text(label)},minLines=if(multiline)3 else 1)}
@Composable private fun SectionTitle(text:String)=Text(text.uppercase(),color=Gold,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
@Composable private fun ParchmentCard(onClick:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit){if(onClick==null)Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF9EE))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}else Card(onClick,Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF9EE))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
