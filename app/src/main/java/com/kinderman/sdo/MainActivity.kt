package com.kinderman.sdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinderman.sdo.data.AttributeValue
import com.kinderman.sdo.data.AuthRepository
import com.kinderman.sdo.data.CharacterEntity
import com.kinderman.sdo.data.CharacterRepository
import com.kinderman.sdo.data.ResourceValue
import com.kinderman.sdo.ui.Acid
import com.kinderman.sdo.ui.AcidCyan
import com.kinderman.sdo.ui.AcidMagenta
import com.kinderman.sdo.ui.AuraBlue
import com.kinderman.sdo.ui.Barcode
import com.kinderman.sdo.ui.Carbon
import com.kinderman.sdo.ui.ComplianceMark
import com.kinderman.sdo.ui.Cyan
import com.kinderman.sdo.ui.DamageTrack
import com.kinderman.sdo.ui.EnergyBlue
import com.kinderman.sdo.ui.Grid
import com.kinderman.sdo.ui.HudBackground
import com.kinderman.sdo.ui.HudTextField
import com.kinderman.sdo.ui.Ice
import com.kinderman.sdo.ui.LabelFunctional
import com.kinderman.sdo.ui.Muted
import com.kinderman.sdo.ui.NeonCoral
import com.kinderman.sdo.ui.Panel
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SectionHeader
import com.kinderman.sdo.ui.Signal
import com.kinderman.sdo.ui.StatHeader
import com.kinderman.sdo.ui.StatHeaderLight
import com.kinderman.sdo.ui.TechCutDark
import com.kinderman.sdo.ui.TechPanel
import com.kinderman.sdo.ui.TelemetryTag
import com.kinderman.sdo.ui.Void
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SdoTheme { SdoApp(this) } }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val repo: CharacterRepository) : ViewModel() {
    private val uid = MutableStateFlow("demo-player")
    private val master = MutableStateFlow(false)
    val isMaster = master.asStateFlow()
    val characters = combine(
        uid,
        master
    ) { user, isMaster -> user to isMaster }.flatMapLatest { repo.observe(it.first, it.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSession(userId: String, isMaster: Boolean) {
        uid.value = userId
        master.value = isMaster
        viewModelScope.launch { repo.sync(userId, isMaster) }
    }

    fun add() = viewModelScope.launch { repo.save(CharacterEntity(ownerId = uid.value)) }
    fun save(character: CharacterEntity) = viewModelScope.launch { repo.save(character) }
}

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    val session = auth.session.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    var state by mutableStateOf(AuthUiState())
        private set
    val configured get() = auth.configured

    fun submitGoogle(activity: MainActivity) = viewModelScope.launch {
        state = AuthUiState(loading = true)
        state = runCatching { auth.loginWithGoogle(activity) }.fold(
            onSuccess = { AuthUiState() },
            onFailure = {
                AuthUiState(
                    error = it.localizedMessage
                        ?: "Falha no protocolo de autenticação. Tente novamente.",
                )
            },
        )
    }

    fun logout() = auth.logout()
    suspend fun master(uid: String) = runCatching { auth.isMaster(uid) }.getOrDefault(false)
}

@Composable
fun SdoApp(activity: MainActivity) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SdoApplication
    val vm: AppViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(app.repository) as T
    })
    val authVm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(AuthRepository()) as T
    })
    val user by authVm.session.collectAsStateWithLifecycle()
    var demo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(user?.uid, demo) {
        if (demo) vm.setSession("demo-player", false)
        else user?.let { vm.setSession(it.uid, authVm.master(it.uid)) }
    }

    if (user == null && !demo) {
        LoginScreen(activity, authVm, allowDemo = !authVm.configured) { demo = true }
        return
    }

    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val characters by vm.characters.collectAsStateWithLifecycle()
    val isMaster by vm.isMaster.collectAsStateWithLifecycle()
    if (selected == null) {
        Dashboard(
            characters = characters,
            master = isMaster,
            onAdd = vm::add,
            onOpen = { selected = it },
            onLogout = {
                demo = false
                authVm.logout()
            },
        )
    } else {
        CharacterSheet(
            character = characters.firstOrNull { it.id == selected },
            master = isMaster,
            back = { selected = null },
            save = vm::save,
        )
    }
}

@Composable
private fun LoginScreen(
    activity: MainActivity,
    vm: AuthViewModel,
    allowDemo: Boolean,
    onDemo: () -> Unit,
) {
    HudBackground {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryTag("SYS.26 // ONLINE")
                TelemetryTag("AUTH_GATE", Signal)
            }
            TechPanel(accent = Acid) {
                Text("SOLIDÃO DOS", color = Acid, style = MaterialTheme.typography.labelLarge)
                Text("OPRIMIDOS", style = MaterialTheme.typography.displayLarge)
                Text(
                    "COMPANION // TERMINAL DE PERSONAGEM",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                )
                Barcode("SDO-COMPANION-AUTH")
                Text(
                    "IDENTIFIQUE-SE PARA ACESSAR ARQUIVOS LOCAIS E SINCRONIZAR A TELEMETRIA DA FICHA.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                vm.state.error?.let {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Signal)
                            .background(Signal.copy(alpha = .12f))
                            .padding(10.dp),
                    ) {
                        Text(
                            "ERR_AUTH // $it",
                            color = Signal,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Button(
                    onClick = { vm.submitGoogle(activity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !vm.state.loading,
                    shape = CutCornerShape(topEnd = 15.dp, bottomStart = 15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Acid, contentColor = Void
                    ),
                ) {
                    if (vm.state.loading) {
                        CircularProgressIndicator(
                            Modifier.size(22.dp), color = Void, strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, null)
                        Spacer(Modifier.size(9.dp))
                        Text("INICIAR COM GOOGLE", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (allowDemo) {
                    HorizontalDivider(color = Grid)
                    OutlinedButton(
                        onClick = onDemo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                    ) { Text("MODO LOCAL // OFFLINE") }
                }
                ComplianceMark()
            }
        }
    }
}

@Composable
private fun Dashboard(
    characters: List<CharacterEntity>,
    master: Boolean,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
) {
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = Acid,
                    contentColor = Void,
                    shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                ) { Icon(Icons.Default.Add, "Criar personagem") }
            },
        ) { padding ->
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TelemetryTag(if (master) "MASTER_ACCESS" else "PLAYER_ACCESS")
                        IconButton(onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout, "Sair", tint = Signal
                            )
                        }
                    }
                    Text("SDO", color = Acid, style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (master) "PAINEL DA MESTRE" else "ARQUIVOS DE CAMPO",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Ice
                    )
                    Text(
                        "LOCAL_CACHE // FIREBASE_SYNC // ${
                            characters.size.toString().padStart(2, '0')
                        } REGISTROS",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (characters.isEmpty()) {
                    item {
                        TechPanel(accent = Signal) {
                            SectionHeader("00", "Nenhum sinal detectado")
                            Text("Crie o primeiro personagem no comando +.")
                            Barcode("EMPTY-SDO-ARCHIVE")
                        }
                    }
                }
                items(characters, key = { it.id }) { character ->
                    CharacterAccessCard(character, master) { onOpen(character.id) }
                }
            }
        }
    }
}

@Composable
private fun CharacterAccessCard(
    character: CharacterEntity,
    master: Boolean,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (character.dirty) Signal else TechCutDark,
                CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
            ),
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TelemetryTag("ID.${character.id.take(6)}")
                TelemetryTag(
                    if (character.dirty) "LOCAL_DELTA" else "SYNC_OK",
                    if (character.dirty) Signal else AcidCyan,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .background(Acid, CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        character.name.take(2).uppercase(),
                        color = Void,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.size(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        character.name.uppercase(),
                        color = Ice,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        listOf(
                            character.race,
                            character.occupation,
                            "LV.${character.level}"
                        ).filter { it.isNotBlank() }.joinToString(" // "),
                        color = LabelFunctional,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Icon(
                    if (master) Icons.Default.AdminPanelSettings else Icons.Default.ChevronRight,
                    null,
                    tint = Acid,
                )
            }
            Barcode(character.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterSheet(
    character: CharacterEntity?,
    master: Boolean,
    back: () -> Unit,
    save: (CharacterEntity) -> Unit,
) {
    if (character == null) return
    var current by remember(character.id, character.updatedAt) { mutableStateOf(character) }
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                current.name.uppercase(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (master) "MASTER_READWRITE // LIVE" else "PLAYER_FILE // LIVE",
                                color = Acid,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    },
                    actions = {
                        IconButton({ save(current) }) {
                            Icon(
                                Icons.Default.Save, "Salvar", tint = Acid
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Void,
                        titleContentColor = Ice,
                        navigationIconContentColor = Ice,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                item { SheetHero(current, master) }
                item {
                    TechPanel {
                        SectionHeader("01", "Identidade")
                        HudTextField("Nome operacional", current.name) {
                            current = current.copy(name = it)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HudTextField("Raça", current.race, Modifier.weight(1f)) {
                                current = current.copy(race = it)
                            }
                            HudTextField("Sub-raça", current.subRace, Modifier.weight(1f)) {
                                current = current.copy(subRace = it)
                            }
                        }
                        HudTextField("Ocupação", current.occupation) {
                            current = current.copy(occupation = it)
                        }
                    }
                }
                item {
                    SectionHeader("02", "Telemetria vital")
                    ResourceGrid(current)
                }
                item { SectionHeader("03", "Atributos e conhecimentos") }
                items(current.attributes, key = { it.acronym }) { AttributeCard(it) }
                item {
                    TechPanel(accent = Cyan) {
                        SectionHeader("04", "Matriz de proteção")
                        current.protections.entries.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (name, value) ->
                                    ProtectionCell(name, value, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    TechPanel(accent = Signal) {
                        SectionHeader("05", "Caminho")
                        HudTextField("Designação", current.pathName) {
                            current = current.copy(pathName = it)
                        }
                        HudTextField("Lema / diretiva", current.pathMotto) {
                            current = current.copy(pathMotto = it)
                        }
                    }
                }
                item {
                    TechPanel {
                        SectionHeader("06", "Memória de campo")
                        HudTextField("Origem do personagem", current.story, multiline = true) {
                            current = current.copy(story = it)
                        }
                        HudTextField("Anotações da mesa", current.notes, multiline = true) {
                            current = current.copy(notes = it)
                        }
                    }
                }
                item {
                    Button(
                        onClick = { save(current) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Acid, contentColor = Void
                        ),
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(Modifier.size(8.dp))
                        Text("SALVAR // SINCRONIZAR", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        "ROOM_LOCAL → FIRESTORE_REMOTE // FAILSAFE ATIVO",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHero(character: CharacterEntity, master: Boolean) {
    TechPanel(accent = Signal) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TelemetryTag(if (master) "OVERRIDE.M" else "PROFILE.P")
            TelemetryTag("LV.${character.level}", Signal)
        }
        Text("ARQUIVO", color = Acid, style = MaterialTheme.typography.labelLarge)
        Text(
            character.name.uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            fontStyle = FontStyle.Italic,
            color = Ice
        )
        Barcode("${character.id}-${character.name}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ComplianceMark()
            Column(horizontalAlignment = Alignment.End) {
                Icon(Icons.Default.CloudDone, null, tint = AcidCyan)
                Text(
                    "CACHE PROTEGIDO",
                    color = AcidCyan,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ResourceGrid(character: CharacterEntity) {
    val entries = listOf(
        Triple("VIDA", character.life, StatHeaderLight),
        Triple("SANIDADE", character.sanity, StatHeader),
        Triple("ARCANO", character.arcane, AuraBlue),
        Triple("ENERGIA", character.energy, EnergyBlue),
        Triple("DESTINO", character.destiny, AcidCyan),
        Triple("EXAUSTÃO", character.exhaustion, NeonCoral),
        Triple("CORRUPÇÃO", character.corruption, AcidMagenta),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, value, color) ->
                    ResourceCell(name, value, color, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResourceCell(
    name: String,
    value: ResourceValue,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (value.maximum > 0) {
        (value.current.toFloat() / value.maximum).coerceIn(0f, 1f)
    } else 0f
    val trackColor = when (name) {
        "CORRUPÇÃO", "EXAUSTÃO", "VIDA" -> DamageTrack
        else -> TechCutDark
    }
    Column(
        modifier
            .border(1.dp, color.copy(alpha = .72f), CutCornerShape(topEnd = 13.dp))
            .background(Carbon)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(name, color = color, style = MaterialTheme.typography.labelSmall)
        Text(
            "${value.current.toString().padStart(2, '0')} / ${
                value.maximum.toString().padStart(2, '0')
            }",
            color = Ice,
            style = MaterialTheme.typography.titleLarge,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = color,
            trackColor = trackColor,
        )
    }
}

@Composable
private fun AttributeCard(attribute: AttributeValue) {
    TechPanel(accent = TechCutDark) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(Acid, CutCornerShape(topEnd = 15.dp, bottomStart = 15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(attribute.acronym, color = Void, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    attribute.name.uppercase(),
                    color = Ice,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "BASE.${
                        attribute.value.toString().padStart(2, '0')
                    } // MOD.${signed(attribute.modifier)}",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TelemetryTag(
                signed(attribute.modifier),
                if (attribute.modifier >= 0) AcidCyan else NeonCoral
            )
        }
        HorizontalDivider(color = TechCutDark)
        attribute.skills.forEachIndexed { index, skill ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${(index + 1).toString().padStart(2, '0')}  ${skill.name.uppercase()}",
                    color = LabelFunctional,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${skill.value.toString().padStart(2, '0')} // ${signed(skill.modifier)}",
                    color = if (skill.modifier >= 0) AcidCyan else NeonCoral,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ProtectionCell(name: String, value: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Carbon)
            .border(1.dp, TechCutDark)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name.uppercase(), color = LabelFunctional, style = MaterialTheme.typography.labelSmall)
        Text(
            value.toString().padStart(2, '0'),
            color = AcidCyan,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()
